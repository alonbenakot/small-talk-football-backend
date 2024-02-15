package com.smalltalk.SmallTalkFootball.services;

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
        String alertMsg = Messages.WELCOME_MEMBER.formatted(user.getFirstName());
        SmallTalkResponse<User> response = new SmallTalkResponse<>(repository.save(user), alertMsg);

        return processResponse(response);
    }

    public User getUserByEmail(String email) {
        Optional<User> userOptional = repository.findByEmail(email);
        return userOptional.orElse(null);
    }


    public SmallTalkResponse<User> login(String email, String password) throws UserException {
        User user = repository.findByEmailAndPassword(email, password)
                .orElseThrow(() -> new UserException(Messages.INCORRECT_EMAIL_OR_PASSWORD));

        SmallTalkResponse<User> response = new SmallTalkResponse<>(user);

        return processResponse(response);
    }

    private SmallTalkResponse<User> processResponse(SmallTalkResponse<User> response) {
        response.setJwt(jwtUtil.generateToken(response.getData()));
        response.getData().setPassword(null);
        return response;
    }
}

