package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.models.UserIndications;
import com.smalltalk.SmallTalkFootball.repositories.ArticleRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.articles.ArticleAlreadyExistsException;
import com.smalltalk.SmallTalkFootball.system.exceptions.articles.NoArticleFoundException;
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

    public Article getArticleById(String id) throws NoArticleFoundException {
        Optional<Article> optional = articleRepo.findById(id);
        return optional.orElseThrow(NoArticleFoundException::new);
    }

    public Article addArticle(Article article) throws ArticleAlreadyExistsException {
        if (articleRepo.findByTitle(article.getTitle()).isPresent()) {
            throw new ArticleAlreadyExistsException();
        }
        article.setPublished(false);
        List<User> admins = userService.getAdmins();
        admins.forEach((admin -> admin.setUserIndications(new UserIndications(true))));
        return articleRepo.save(article);
    }

    public Article publishArticle(String id) throws NoArticleFoundException {
        Article article = articleRepo.findById(id).orElseThrow(NoArticleFoundException::new);
        article.setPublished(true);
        return articleRepo.save(article);
    }

    public Article updateArticle(Article article) throws NoArticleFoundException {
        Article articleFromDb = articleRepo.findById(article.getId()).orElseThrow(NoArticleFoundException::new);
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

    public void deleteArticle(String id) throws NoArticleFoundException {
        if (articleRepo.existsById(id)) {
            articleRepo.deleteById(id);
        } else {
            throw new NoArticleFoundException();
        }
    }


}
