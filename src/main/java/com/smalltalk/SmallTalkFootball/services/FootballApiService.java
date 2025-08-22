package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FootballApiService {
    private static final String BASE_URL = "https://apiv3.apifootball.com/";

    private final RestClient restClient;

    private final String apiKey;

    public FootballApiService(RestClient.Builder builder, @Value("${api.football.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    public List<MatchDto> getMatches(LocalDate fromDate, Competition competition) {
        return restClient.get().uri(uriBuilder -> uriBuilder
                        .queryParam("APIkey", apiKey)
                        .queryParam("action", "get_events")
                        .queryParam("from", fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .queryParam("to", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .queryParam("league_id", competition.getCode())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<TeamDataDto> getTeamData(Competition competition) {
        return restClient.get().uri(uriBuilder -> uriBuilder
                        .queryParam("APIkey", apiKey)
                        .queryParam("action", "get_teams")
                        .queryParam("league_id", competition.getCode())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

}

