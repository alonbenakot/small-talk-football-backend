package com.smalltalk.SmallTalkFootball.listeners;

import com.smalltalk.SmallTalkFootball.events.UserCreatedEvent;
import com.smalltalk.SmallTalkFootball.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        emailService.sendWelcomeMail(event.getUser());
    }

}
