package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private User user;

    private String message;

}
