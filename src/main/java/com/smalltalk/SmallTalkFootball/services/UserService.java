package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.entities.User;
import com.smalltalk.SmallTalkFootball.repositories.UserRepository;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import com.smalltalk.SmallTalkFootball.system.messages.ErrorMessages;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;

    public SmallTalkResponse<User> addUser(User user) throws UserException {
        if (repository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserException(ErrorMessages.MEMBER_WITH_EMAIL_EXISTS);
        }
        String alertMsg = "Welcome to the team, " + user.getFirstName() + "!";
        return new SmallTalkResponse<>(repository.save(user), alertMsg);
    }

    public SmallTalkResponse<User> login(String email, String password) throws UserException {
        User user = repository.findByEmailAndPassword(email, password)
                .orElseThrow(() -> new UserException(ErrorMessages.INCORRECT_EMAIL_OR_PASSWORD));

        return new SmallTalkResponse<>(user);
    }
}
