package com.smalltalk.SmallTalkFootball.models;

public class LoginInput {

    private String email;

    private String password;

    public LoginInput(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
