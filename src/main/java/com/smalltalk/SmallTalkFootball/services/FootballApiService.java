package com.smalltalk.SmallTalkFootball.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class FootballApiService {

    private final WebClient webClient;

    public FootballApiService(WebClient.Builder builder, @Value("${api.token}") String apiToken) {
        this.webClient = builder.baseUrl("https://api.football-data.org/v4/")
                .defaultHeader("X-Auth-Token", apiToken)
                .build();
    }


}
