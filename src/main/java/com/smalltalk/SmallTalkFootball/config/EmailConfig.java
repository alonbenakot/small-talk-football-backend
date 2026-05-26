package com.smalltalk.SmallTalkFootball.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {

    @Bean
    public Resend resendClient(@Value("${email.api.key}") String apiKey) {
        return new Resend(apiKey);
    }
}
