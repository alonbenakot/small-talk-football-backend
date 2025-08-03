package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.Article;

import java.nio.file.Path;
import java.util.List;

public class ArticleReader {

    private static final String ARTICLES_BASE_PATH = "src/main/resources/data/articles/";

    public static List<Article> read() {
        return JsonReader.read(getBasePath(), Article.class);
    }

    private static Path getBasePath() {
        return Path.of(ARTICLES_BASE_PATH);
    }

}
