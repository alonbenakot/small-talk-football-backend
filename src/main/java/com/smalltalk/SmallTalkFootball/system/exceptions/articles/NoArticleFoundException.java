package com.smalltalk.SmallTalkFootball.system.exceptions.articles;

public class NoArticleFoundException extends ArticleException{
    private static final String ERROR_MSG = "No article was found.";

    public NoArticleFoundException() {
        super(ERROR_MSG);
    }
}
