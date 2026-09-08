package com.smalltalk.SmallTalkFootball.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.models.LoginInput;
import com.smalltalk.SmallTalkFootball.models.UserResponse;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.security.JwtUtil;
import com.smalltalk.SmallTalkFootball.services.UserService;
import com.smalltalk.SmallTalkFootball.system.exceptions.UserException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService service;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.generateToken(any())).thenReturn("a-signed-token");
    }

    private String signupBody() throws Exception {
        return objectMapper.writeValueAsString(TestFixtures.member("ada@example.com"));
    }

    @Test
    void returnsATokenOnSignup() throws Exception {
        User created = TestFixtures.member("ada@example.com");
        when(service.addUser(any())).thenReturn(new UserResponse(created, "Welcome to the team, Ada!"));

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jwt").value("a-signed-token"))
                .andExpect(jsonPath("$.systemMessage.messageText").value("Welcome to the team, Ada!"));
    }

    /**
     * The password must never travel back to the client, even though it is stored in plain text.
     */
    @Test
    void neverEchoesThePasswordBack() throws Exception {
        User created = TestFixtures.member("ada@example.com");
        when(service.addUser(any())).thenReturn(new UserResponse(created, "Welcome"));

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("ada@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void returnsATokenOnLogin() throws Exception {
        User existing = TestFixtures.member("ada@example.com");
        when(service.login(any())).thenReturn(new UserResponse(existing, "Hey Ada, great to have you back!"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginInput("ada@example.com", "s3cret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("a-signed-token"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void reportsBadCredentialsAsABadRequest() throws Exception {
        when(service.login(any())).thenThrow(new UserException(Messages.INCORRECT_EMAIL_OR_PASSWORD));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginInput("ada@example.com", "wrong"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.systemMessage.messageText").value(Messages.INCORRECT_EMAIL_OR_PASSWORD))
                .andExpect(jsonPath("$.jwt").doesNotExist());
    }

    @Test
    void reportsADuplicateEmailAsABadRequest() throws Exception {
        when(service.addUser(any())).thenThrow(new UserException(Messages.MEMBER_WITH_EMAIL_EXISTS));

        mockMvc.perform(post("/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.systemMessage.messageText").value(Messages.MEMBER_WITH_EMAIL_EXISTS));
    }
}
