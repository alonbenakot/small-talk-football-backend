package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.security.JwtUtil;
import com.smalltalk.SmallTalkFootball.models.LoginInput;
import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.models.UserResponse;
import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("users/")
public class UserController {

    private final UserService service;
    private final JwtUtil jwtUtil;

    @PostMapping("signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<User> signUp(@RequestBody User user) throws SmallTalkException {
        UserResponse userResponse = service.addUser(user);
        SmallTalkResponse<User> response = new SmallTalkResponse<>(userResponse.getUser(), userResponse.getMessage());
        response.setStatusCode(HttpStatus.CREATED.value());

        return processResponse(response);
    }

    @PostMapping("login")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<User> login(@RequestBody LoginInput loginInput) throws UserException {
        UserResponse userResponse = service.login(loginInput);
        SmallTalkResponse<User> response = new SmallTalkResponse<>(userResponse.getUser(), userResponse.getMessage());
        response.setStatusCode(HttpStatus.OK.value());

        return processResponse(response);
    }

    private SmallTalkResponse<User> processResponse(SmallTalkResponse<User> response) {
        response.setJwt(jwtUtil.generateToken(response.getData()));
        response.getData().setPassword(null);
        return response;
    }
}
