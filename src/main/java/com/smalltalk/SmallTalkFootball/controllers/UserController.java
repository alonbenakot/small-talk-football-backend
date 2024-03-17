package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.entities.User;
import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<User> signUp(@RequestBody User user) throws SmallTalkException {
        SmallTalkResponse<User> response = service.addUser(user);
        response.setStatusCode(HttpStatus.CREATED.value());
        return response;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<User> login(@RequestParam String email, @RequestParam String password) throws UserException {
        SmallTalkResponse<User> response = service.login(email, password);
        response.setStatusCode(HttpStatus.OK.value());
        return response;
    }
}
