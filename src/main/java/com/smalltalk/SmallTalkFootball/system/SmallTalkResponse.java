package com.smalltalk.SmallTalkFootball.system;

import com.smalltalk.SmallTalkFootball.system.messages.SystemMessage;

public class SmallTalkResponse<T> {

    public T data;
    public SystemMessage systemMessage;

    public SmallTalkResponse(String errorMsg) {
        this.data = null;
        this.systemMessage = new SystemMessage(errorMsg);
    }

    public SmallTalkResponse(T data) {
        this.data = data;
        this.systemMessage = new SystemMessage();
    }

    public SmallTalkResponse(T data, String alertMsg) {
        this.data = data;
        this.systemMessage = new SystemMessage(alertMsg, false);
    }
}
