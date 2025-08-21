package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.models.dto.MatchesByCompetitionsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FootballApiService {
    private static final String BASE_URL = "https://api.football-data.org/v4/";
    private static final String COMPETITIONS_MATCHES_URI = "competitions/{id}/matches";

    private final RestClient restClient;

    public FootballApiService(RestClient.Builder builder, @Value("${api.token}") String apiToken) {
        restClient = builder.baseUrl(BASE_URL)
                .defaultHeader("X-Auth-Token", apiToken)
                .build();
    }

    public MatchesByCompetitionsDTO getMatches(String competitionId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(COMPETITIONS_MATCHES_URI)
//                        .queryParam("status", "FINISHED")
                        .build(competitionId))
                .header("X-Unfold-Goals", Boolean.TRUE.toString())
                .retrieve()
                .body(MatchesByCompetitionsDTO.class);

    }
}
