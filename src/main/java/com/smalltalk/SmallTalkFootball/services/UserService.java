package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.enums.Role;
import com.smalltalk.SmallTalkFootball.models.LoginInput;
import com.smalltalk.SmallTalkFootball.models.UserIndications;
import com.smalltalk.SmallTalkFootball.models.UserResponse;
import com.smalltalk.SmallTalkFootball.repositories.UserRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;



    public UserResponse addUser(User user) throws UserException {
        if (repository.existsByEmail(user.getEmail())) {
            throw new UserException(Messages.MEMBER_WITH_EMAIL_EXISTS);
        }

        user.setRole(Role.MEMBER);
        user.setUserIndications(new UserIndications());

        return new UserResponse(repository.save(user), Messages.WELCOME_MEMBER.formatted(user.getFirstName()));
    }

    public User getUserByEmail(String email) {
        Optional<User> userOptional = repository.findByEmail(email);
        return userOptional.orElse(null);
    }

    public UserResponse login(LoginInput loginInput) throws UserException {
        User user = repository
                .findByEmailAndPassword(loginInput.getEmail(), loginInput.getPassword())
                .orElseThrow(() -> new UserException(Messages.INCORRECT_EMAIL_OR_PASSWORD));

        return new UserResponse(user, Messages.MEMBER_LOGIN.formatted(user.getFirstName()));
    }

    public void setPendingArticleIndication(boolean isPending) {
        getAdmins().forEach(admin -> {
            admin.getUserIndications().setPendingArticles(isPending);
            repository.save(admin);
        });
    }

    private List<User> getAdmins() {
        return repository.findAllByRole(Role.ADMIN);
    }
}

