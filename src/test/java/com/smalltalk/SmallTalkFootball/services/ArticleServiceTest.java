package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.repositories.ArticleRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.ArticleException;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository repository;
    @Mock
    private UserService userService;

    private ArticleService service;

    @BeforeEach
    void setUp() {
        service = new ArticleService(repository, userService);
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Article submitted(String title, String text) {
        Article article = TestFixtures.article(null, title, false);
        article.setText(text);
        return article;
    }

    @Nested
    class Adding {

        @Test
        void savesANewArticleAsUnpublished() throws Exception {
            when(repository.findByTitle("A Title")).thenReturn(Optional.empty());
            Article submitted = submitted("A Title", "Body");
            submitted.setPublished(true);

            Article saved = service.addArticle(submitted);

            assertThat(saved.isPublished()).isFalse();
            verify(repository).save(submitted);
        }

        @Test
        void flagsPendingArticlesForAdmins() throws Exception {
            when(repository.findByTitle(any())).thenReturn(Optional.empty());

            service.addArticle(submitted("A Title", "Body"));

            verify(userService).setPendingArticleIndication(true);
        }

        @Test
        void rejectsADuplicateTitle() {
            when(repository.findByTitle("Taken")).thenReturn(Optional.of(TestFixtures.article("1", "Taken", true)));

            assertThatThrownBy(() -> service.addArticle(submitted("Taken", "Body")))
                    .isInstanceOf(ArticleException.class)
                    .hasMessage(Messages.ARTICLE_TITLE_NOT_UNIQUE);

            verify(repository, never()).save(any());
        }

        @Test
        void promotesSingleLineBreaksToParagraphBreaks() throws Exception {
            when(repository.findByTitle(any())).thenReturn(Optional.empty());

            Article saved = service.addArticle(submitted("A Title", "First line\nSecond line"));

            assertThat(saved.getText()).isEqualTo("First line\n\nSecond line");
        }

        @Test
        void leavesExistingParagraphBreaksAlone() throws Exception {
            when(repository.findByTitle(any())).thenReturn(Optional.empty());

            Article saved = service.addArticle(submitted("A Title", "First para\n\nSecond para"));

            assertThat(saved.getText()).isEqualTo("First para\n\nSecond para");
        }

        @Test
        void normalisesWindowsAndClassicMacLineEndings() throws Exception {
            when(repository.findByTitle(any())).thenReturn(Optional.empty());

            Article saved = service.addArticle(submitted("A Title", "First\r\nSecond\rThird"));

            assertThat(saved.getText()).isEqualTo("First\n\nSecond\n\nThird");
        }
    }

    @Nested
    class Publishing {

        @Test
        void publishesAPendingArticle() throws Exception {
            Article pending = TestFixtures.article("1", "Pending", false);
            when(repository.findById("1")).thenReturn(Optional.of(pending));
            when(repository.findAllByPublishedFalse()).thenReturn(List.of());

            Article published = service.publishArticle("1");

            assertThat(published.isPublished()).isTrue();
            verify(repository).save(pending);
        }

        @Test
        void clearsThePendingFlagOnceNothingIsLeftToReview() throws Exception {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Pending", false)));
            when(repository.findAllByPublishedFalse()).thenReturn(List.of());

            service.publishArticle("1");

            verify(userService).setPendingArticleIndication(false);
        }

        @Test
        void keepsThePendingFlagWhileOtherArticlesAwaitReview() throws Exception {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Pending", false)));
            when(repository.findAllByPublishedFalse())
                    .thenReturn(List.of(TestFixtures.article("2", "Also Pending", false)));

            service.publishArticle("1");

            verify(userService).setPendingArticleIndication(true);
        }

        @Test
        void refusesToPublishTwice() {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Live", true)));

            assertThatThrownBy(() -> service.publishArticle("1"))
                    .isInstanceOf(ArticleException.class)
                    .hasMessage(Messages.ARTICLE_ALREADY_PUBLISHED);
        }

        @Test
        void reportsAMissingArticle() {
            when(repository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishArticle("nope"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(Messages.NO_ARTICLE_FOUND);
        }
    }

    @Nested
    class Removing {

        @Test
        void returnsAPublishedArticleToPending() throws Exception {
            Article published = TestFixtures.article("1", "Live", true);
            when(repository.findById("1")).thenReturn(Optional.of(published));

            Article removed = service.removeArticle("1");

            assertThat(removed.isPublished()).isFalse();
            verify(userService).setPendingArticleIndication(true);
        }

        @Test
        void refusesToRemoveAnArticleThatWasNeverPublished() {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Pending", false)));

            assertThatThrownBy(() -> service.removeArticle("1"))
                    .isInstanceOf(ArticleException.class)
                    .hasMessage(Messages.ARTICLE_IS_NOT_PUBLISHED);
        }
    }

    @Nested
    class Updating {

        @Test
        void copiesOnlyTheFieldsThatWereSupplied() throws Exception {
            Article stored = TestFixtures.article("1", "Original", true);
            when(repository.findById("1")).thenReturn(Optional.of(stored));

            Article patch = new Article();
            patch.setId("1");
            patch.setTitle("New Title");
            patch.setPublished(true);

            Article updated = service.updateArticle(patch);

            assertThat(updated.getTitle()).isEqualTo("New Title");
            assertThat(updated.getAuthor()).isEqualTo("System Article");
            assertThat(updated.getText()).isEqualTo("Some body text.");
        }

        /*
         * Pins a defect. `published` is a primitive, so a PATCH body that omits it arrives as
         * false and updateNonNullFields copies it unconditionally - silently unpublishing an
         * article that the caller only meant to retitle.
         */
        @Test
        void silentlyUnpublishesWhenThePatchOmitsThePublishedFlag() throws Exception {
            Article stored = TestFixtures.article("1", "Original", true);
            when(repository.findById("1")).thenReturn(Optional.of(stored));

            Article patch = new Article();
            patch.setId("1");
            patch.setTitle("New Title");

            Article updated = service.updateArticle(patch);

            assertThat(updated.isPublished()).isFalse();
        }

        @Test
        void reportsAMissingArticle() {
            Article patch = new Article();
            patch.setId("nope");
            when(repository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateArticle(patch))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Deleting {

        @Test
        void clearsThePendingFlagWhenDeletingAnUnpublishedArticle() throws Exception {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Pending", false)));

            service.deleteArticle("1");

            verify(userService).setPendingArticleIndication(false);
            verify(repository).deleteById("1");
        }

        @Test
        void leavesThePendingFlagAloneWhenDeletingAPublishedArticle() throws Exception {
            when(repository.findById("1")).thenReturn(Optional.of(TestFixtures.article("1", "Live", true)));

            service.deleteArticle("1");

            verify(userService, never()).setPendingArticleIndication(org.mockito.ArgumentMatchers.anyBoolean());
            verify(repository).deleteById("1");
        }

        @Test
        void reportsAMissingArticle() {
            when(repository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteArticle("nope"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Seeding {

        /**
         * The init endpoint replaces the collection wholesale, so the delete has to happen and
         * the reload has to come from the files on disk.
         */
        @Test
        void replacesTheCollectionFromTheShippedFiles() {
            service.initArticles();

            verify(repository).deleteAll();
            verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(
                    articles -> ((List<Article>) articles).size() > 0));
        }
    }

    @Nested
    class Reading {

        @Test
        void separatesPublishedFromPendingArticles() {
            when(repository.findAllByPublishedTrue()).thenReturn(List.of(TestFixtures.article("1", "Live", true)));
            when(repository.findAllByPublishedFalse()).thenReturn(List.of(TestFixtures.article("2", "Draft", false)));

            assertThat(service.getPublishedArticles()).extracting(Article::getTitle).containsExactly("Live");
            assertThat(service.getPendingArticles()).extracting(Article::getTitle).containsExactly("Draft");
        }

        @Test
        void reportsAMissingArticle() {
            when(repository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getArticleById("nope"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(Messages.NO_ARTICLE_FOUND);
        }
    }
}
