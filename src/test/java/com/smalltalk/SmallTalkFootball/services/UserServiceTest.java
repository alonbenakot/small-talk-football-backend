package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Role;
import com.smalltalk.SmallTalkFootball.events.UserCreatedEvent;
import com.smalltalk.SmallTalkFootball.models.LoginInput;
import com.smalltalk.SmallTalkFootball.models.UserIndications;
import com.smalltalk.SmallTalkFootball.models.UserResponse;
import com.smalltalk.SmallTalkFootball.repositories.UserRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<UserCreatedEvent> publishedEvent;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, eventPublisher);
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static User signingUp(String email, Language preferredLanguage) {
        User user = new User("Ada", "Lovelace", email, "s3cret", false);
        user.setUserIndications(new UserIndications(true, preferredLanguage));
        return user;
    }

    @Test
    void registersANewMember() throws UserException {
        when(repository.existsByEmail("ada@example.com")).thenReturn(false);

        UserResponse response = service.addUser(signingUp("ada@example.com", Language.BRITISH));

        assertThat(response.getUser().getRole()).isEqualTo(Role.MEMBER);
        assertThat(response.getMessage()).isEqualTo(Messages.WELCOME_MEMBER.formatted("Ada"));
    }

    @Test
    void neverLetsASignupClaimAdmin() throws UserException {
        when(repository.existsByEmail(any())).thenReturn(false);
        User aspiringAdmin = signingUp("ada@example.com", Language.BRITISH);
        aspiringAdmin.setRole(Role.ADMIN);

        UserResponse response = service.addUser(aspiringAdmin);

        assertThat(response.getUser().getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    void keepsThePreferredLanguageButClearsThePendingArticleFlag() throws UserException {
        when(repository.existsByEmail(any())).thenReturn(false);

        UserResponse response = service.addUser(signingUp("ada@example.com", Language.HEBREW));

        assertThat(response.getUser().getUserIndications().getPreferredLanguage()).isEqualTo(Language.HEBREW);
        assertThat(response.getUser().getUserIndications().isPendingArticles()).isFalse();
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        when(repository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.addUser(signingUp("ada@example.com", Language.BRITISH)))
                .isInstanceOf(UserException.class)
                .hasMessage(Messages.MEMBER_WITH_EMAIL_EXISTS);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void announcesTheNewUserSoTheWelcomeEmailCanBeSent() throws UserException {
        when(repository.existsByEmail(any())).thenReturn(false);

        service.addUser(signingUp("ada@example.com", Language.BRITISH));

        verify(eventPublisher).publishEvent(publishedEvent.capture());
        assertThat(publishedEvent.getValue().getUser().getEmail()).isEqualTo("ada@example.com");
    }

    /*
     * Pins a defect. addUser reads user.getUserIndications().getPreferredLanguage() before
     * checking it for null, so a signup payload that omits userIndications fails with a
     * NullPointerException and surfaces as a 500 rather than a validation message.
     */
    @Test
    void failsWithNullPointerWhenTheSignupOmitsUserIndications() {
        when(repository.existsByEmail(any())).thenReturn(false);
        User withoutIndications = new User("Ada", "Lovelace", "ada@example.com", "s3cret", false);

        assertThatThrownBy(() -> service.addUser(withoutIndications))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void logsAMemberIn() throws UserException {
        User stored = TestFixtures.member("ada@example.com");
        when(repository.findByEmailAndPassword("ada@example.com", "s3cret")).thenReturn(Optional.of(stored));

        UserResponse response = service.login(new LoginInput("ada@example.com", "s3cret"));

        assertThat(response.getUser()).isSameAs(stored);
        assertThat(response.getMessage()).isEqualTo(Messages.MEMBER_LOGIN.formatted("Ada"));
    }

    @Test
    void rejectsBadCredentials() {
        when(repository.findByEmailAndPassword(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginInput("ada@example.com", "wrong")))
                .isInstanceOf(UserException.class)
                .hasMessage(Messages.INCORRECT_EMAIL_OR_PASSWORD);
    }

    @Test
    void findsAUserByEmail() {
        User stored = TestFixtures.member("ada@example.com");
        when(repository.findByEmail("ada@example.com")).thenReturn(Optional.of(stored));

        assertThat(service.getUserByEmail("ada@example.com")).isSameAs(stored);
    }

    @Test
    void returnsNullRatherThanThrowingForAnUnknownEmail() {
        when(repository.findByEmail(any())).thenReturn(Optional.empty());

        assertThat(service.getUserByEmail("nobody@example.com")).isNull();
    }

    @Test
    void raisesThePendingArticleFlagOnEveryAdmin() {
        User firstAdmin = TestFixtures.admin("one@example.com");
        User secondAdmin = TestFixtures.admin("two@example.com");
        when(repository.findAllByRole(Role.ADMIN)).thenReturn(List.of(firstAdmin, secondAdmin));

        service.setPendingArticleIndication(true);

        assertThat(firstAdmin.getUserIndications().isPendingArticles()).isTrue();
        assertThat(secondAdmin.getUserIndications().isPendingArticles()).isTrue();
        verify(repository).save(firstAdmin);
        verify(repository).save(secondAdmin);
    }

    @Test
    void doesNothingWhenThereAreNoAdmins() {
        when(repository.findAllByRole(Role.ADMIN)).thenReturn(List.of());

        service.setPendingArticleIndication(true);

        verify(repository, never()).save(any());
    }
}
