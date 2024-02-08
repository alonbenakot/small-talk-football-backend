package com.smalltalk.SmallTalkFootball.system.messages;

import lombok.Getter;

@Getter
public class SystemMessage {

    private String messageText;
    private boolean isError;

    public SystemMessage() {
        this.messageText = null;
        this.isError = false;
    }

    public SystemMessage(String messageText) {
        this.messageText = messageText;
        this.isError = true;
    }

    public SystemMessage(String messageText, boolean isError) {
        this.messageText = messageText;
        this.isError = isError;
    }
}
