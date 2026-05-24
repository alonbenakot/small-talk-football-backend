package com.smalltalk.SmallTalkFootball.system.exceptions;

public class EmailException extends SmallTalkException {

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
