package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.services.ArticleService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.ArticleException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("articles")
@AllArgsConstructor
public class ArticleController {

    private final ArticleService service;

    @GetMapping("/published")
    public ResponseEntity<SmallTalkResponse<List<Article>>> getPublishedArticles() {
        List<Article> publishedArticles = service.getPublishedArticles();
        if (publishedArticles.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        SmallTalkResponse<List<Article>> response = new SmallTalkResponse<>(publishedArticles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<SmallTalkResponse<List<Article>>> getPendingArticles() {
        List<Article> pendingArticles = service.getPendingArticles();
        if (pendingArticles.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        SmallTalkResponse<List<Article>> response = new SmallTalkResponse<>(pendingArticles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<Article> getArticle(@PathVariable String id) throws NotFoundException {
        return new SmallTalkResponse<>(service.getArticleById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<Article> addArticle(@RequestBody Article article) throws ArticleException {
        return new SmallTalkResponse<>(service.addArticle(article));
    }

    @PatchMapping("/publish/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<Article> publishArticle(@PathVariable String id) throws  ArticleException, NotFoundException {
        return new SmallTalkResponse<>(service.publishArticle(id));
    }

    @PatchMapping()
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<Article> updateArticle(@RequestBody Article article) throws NotFoundException {
        return new SmallTalkResponse<>(service.updateArticle(article));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable String id) throws NotFoundException {
        service.deleteArticle(id);
    }
}
