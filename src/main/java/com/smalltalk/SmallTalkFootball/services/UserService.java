package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.entities.LoginInput;
import com.smalltalk.SmallTalkFootball.entities.User;
import com.smalltalk.SmallTalkFootball.repositories.UserRepository;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.config.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final JwtUtil jwtUtil;

    public SmallTalkResponse<User> addUser(User user) throws UserException {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserException(Messages.MEMBER_WITH_EMAIL_EXISTS);
        }
        String msg = Messages.WELCOME_MEMBER.formatted(user.getFirstName());
        SmallTalkResponse<User> response = new SmallTalkResponse<>(repository.save(user), msg);

        return processResponse(response);
    }

    public User getUserByEmail(String email) {
        Optional<User> userOptional = repository.findByEmail(email);
        return userOptional.orElse(null);
    }


    public SmallTalkResponse<User> login(LoginInput loginInput) throws UserException {
        User user = repository.findByEmailAndPassword(loginInput.getEmail(), loginInput.getPassword())
                .orElseThrow(() -> new UserException(Messages.INCORRECT_EMAIL_OR_PASSWORD));
        String msg = Messages.MEMBER_LOGIN.formatted(user.getFirstName());
        SmallTalkResponse<User> response = new SmallTalkResponse<>(user,msg);

        return processResponse(response);
    }

    private SmallTalkResponse<User> processResponse(SmallTalkResponse<User> response) {
        response.setJwt(jwtUtil.generateToken(response.getData()));
        response.getData().setPassword(null);
        return response;
    }
}

