package com.smalltalk.SmallTalkFootball.repositories;

import com.smalltalk.SmallTalkFootball.entities.Article;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    List<Article> findAllByPublishedFalse();

    List<Article> findAllByPublishedTrue();

    Optional<Article> findByTitle(String title);
}
