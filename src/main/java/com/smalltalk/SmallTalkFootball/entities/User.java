package com.smalltalk.SmallTalkFootball.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
public class User {

    @Id
    private String id;

    private String firstName;

    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String password;

    private boolean priorFootballKnowledge;

    public User(String firstName, String lastName, String email, String password, boolean priorFootballKnowledge) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.priorFootballKnowledge = priorFootballKnowledge;
    }

    public User(String firstName, String lastName, String email, boolean priorFootballKnowledge) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.priorFootballKnowledge = priorFootballKnowledge;
    }
}
