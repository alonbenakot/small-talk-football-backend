package com.smalltalk.SmallTalkFootball.system.messages;

import lombok.Getter;

@Getter
public class SystemMessage {

    private String messageText;
    private boolean error;

    public SystemMessage() {
        this.messageText = null;
        this.error = false;
    }

    public SystemMessage(String messageText) {
        this.messageText = messageText;
        this.error = true;
    }

    public SystemMessage(String messageText, boolean error) {
        this.messageText = messageText;
        this.error = error;
    }
}
