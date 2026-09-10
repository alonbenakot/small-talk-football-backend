package com.smalltalk.SmallTalkFootball.enums;

public enum Language {

    BRITISH("British English"),
    AMERICAN("American English"),
    HEBREW("Hebrew");

    private final String description;

    Language(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
