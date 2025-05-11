package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.repositories.ArticleRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.ArticleException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepo;
    private final UserService userService;

    public List<Article> getPublishedArticles() {
        return articleRepo.findAllByPublishedTrue();
    }

    public List<Article> getPendingArticles() {
        return articleRepo.findAllByPublishedFalse();
    }

    public Article getArticleById(String id) throws NotFoundException {
        Optional<Article> optional = articleRepo.findById(id);
        return optional.orElseThrow(() -> new NotFoundException(Messages.NO_ARTICLE_FOUND));
    }

    public Article addArticle(Article article) throws ArticleException {
        if (articleRepo.findByTitle(article.getTitle()).isPresent()) {
            throw new ArticleException(Messages.ARTICLE_TITLE_NOT_UNIQUE);
        }
        article.setPublished(false);
        userService.setPendingArticleIndication(true);
        return articleRepo.save(article);
    }

    public Article publishArticle(String id) throws ArticleException, NotFoundException {
        Article article = articleRepo.findById(id).orElseThrow(() -> new NotFoundException(Messages.NO_ARTICLE_FOUND));
        if (article.isPublished()) {
            throw new ArticleException(Messages.ARTICLE_ALREADY_PUBLISHED);
        }
        article.setPublished(true);
        articleRepo.save(article);
        userService.setPendingArticleIndication(false);
        return article;
    }

    public Article updateArticle(Article article) throws NotFoundException {
        Article articleFromDb = articleRepo.findById(article.getId()).orElseThrow(() -> new NotFoundException(Messages.NO_ARTICLE_FOUND));
        return articleRepo.save(updateNonNullFields(articleFromDb, article));
    }

    private Article updateNonNullFields(Article articleFromDb, Article article) {
        if (article.getTitle() != null) {
            articleFromDb.setTitle(article.getTitle());
        }
        if (article.getAuthor() != null) {
            articleFromDb.setAuthor(article.getAuthor());
        }
        if (article.getText() != null) {
            articleFromDb.setText(article.getText());
        }
        articleFromDb.setPublished(article.isPublished());

        return articleFromDb;
    }

    public void deleteArticle(String id) throws NotFoundException {
        Article article = articleRepo.findById(id).orElseThrow(() -> new NotFoundException(Messages.NO_ARTICLE_FOUND));
        if (!article.isPublished()) {
            userService.setPendingArticleIndication(false);
        }
        articleRepo.deleteById(id);
    }


}
