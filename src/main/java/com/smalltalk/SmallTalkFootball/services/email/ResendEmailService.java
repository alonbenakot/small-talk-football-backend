package com.smalltalk.SmallTalkFootball.services.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Template;
import com.smalltalk.SmallTalkFootball.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResendEmailService implements EmailService {

    private final Resend resendClient;

    @Override
    public void sendWelcomeMail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("MEMBER_NAME", user.getFirstName());

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Small Talk Football <noreply@small-talk-football.com>")
                .to(user.getEmail())
                .subject("Welcome to Small Talk Football!")
                .template(Template.builder()
                        .id("welcome-mail")
                        .variables(variables)
                        .build())
                .build();

        try {
            log.info("Sending email to {}", user.getEmail());
            CreateEmailResponse response = resendClient.emails().send(params);
            log.info("Welcome email sent to {} with ID: {}", user.getEmail(), response.getId());
        } catch (ResendException e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void sendArticleUpForReviewMail() {
        // TODO: Implement article review notification
        log.warn("sendArticleUpForReviewMail not yet implemented");
    }

    @Override
    public void sendArticleAvailableForReviewMail() {
        // TODO: Implement article available for review notification
        log.warn("sendArticleAvailableForReviewMail not yet implemented");
    }

    @Override
    public void sendArticlePublishedMail() {
        // TODO: Implement article published notification
        log.warn("sendArticlePublishedMail not yet implemented");
    }
}
