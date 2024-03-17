package com.smalltalk.SmallTalkFootball.system;

import com.smalltalk.SmallTalkFootball.system.messages.SystemMessage;
import lombok.Getter;

@Getter
public class SmallTalkResponse<T> {

    private T data;
    private SystemMessage systemMessage;
    private String jwt;
    private int statusCode;


    public SmallTalkResponse(String errorMsg) {
        this.data = null;
        this.systemMessage = new SystemMessage(errorMsg);
    }

    public SmallTalkResponse(String errorMsg, int statusCode) {
        this.data = null;
        this.systemMessage = new SystemMessage(errorMsg);
        this.statusCode = statusCode;
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

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
