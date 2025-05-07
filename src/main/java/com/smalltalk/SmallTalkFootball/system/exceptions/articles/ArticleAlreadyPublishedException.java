package com.smalltalk.SmallTalkFootball.system.exceptions.articles;

public class ArticleAlreadyExistsException extends ArticleException{
    private static final String ERROR_MSG = "An article with the same title already exists. If it is not the same as your article, consider changing the title";

    public ArticleAlreadyExistsException() {
        super(ERROR_MSG);
    }

}
