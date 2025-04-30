package com.smalltalk.SmallTalkFootball;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SmallTalkFootballApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmallTalkFootballApplication.class, args);
    }

}
