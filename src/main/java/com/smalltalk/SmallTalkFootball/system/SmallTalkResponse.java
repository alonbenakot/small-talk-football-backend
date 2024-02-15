package com.smalltalk.SmallTalkFootball.system;

import com.smalltalk.SmallTalkFootball.system.messages.SystemMessage;
import lombok.Getter;

@Getter
public class SmallTalkResponse<T> {

    private T data;
    private SystemMessage systemMessage;
    private String jwt;


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

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
