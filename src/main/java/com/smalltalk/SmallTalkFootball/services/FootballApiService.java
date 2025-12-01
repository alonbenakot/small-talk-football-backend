package com.smalltalk.SmallTalkFootball.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.dto.CompetitionDto;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.system.utils.ResponseHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FootballApiService {
    private static final String BASE_URL = "https://apiv3.apifootball.com/";

    private final RestClient restClient;

    private final String apiKey;

    private final ObjectMapper apiClientObjectMapper;

    public FootballApiService(RestClient.Builder builder, @Value("${api.football.key}") String apiKey,
                              @Qualifier("apiClient") ObjectMapper apiClientObjectMapper) {
        this.apiKey = apiKey;
        this.apiClientObjectMapper = apiClientObjectMapper;
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    public List<MatchDto> getMatches(LocalDate fromDate) {
        return Arrays.stream(Competition.values())
                .flatMap(competition -> fetchMatchesByCompetition(competition, fromDate))
                .toList();
    }

    public List<CompetitionDto> getCompetitionData() {
        return ResponseHandler.process(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("APIkey", apiKey)
                                .queryParam("action", "get_leagues")
                                .build())
                        .retrieve()
                        .toEntity(String.class),
                "Failed fetching competitions data",
                new TypeReference<List<CompetitionDto>>() {
                },
                apiClientObjectMapper
        ).toList();
    }

    public List<TeamDataDto> getTeamData(Competition competition) {
        return ResponseHandler.process(
                        () -> restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .queryParam("APIkey", apiKey)
                                        .queryParam("action", "get_teams")
                                        .queryParam("league_id", competition.getCode())
                                        .build())
                                .retrieve()
                                .toEntity(String.class),
                        "Failed fetching " + competition + " team data",
                        new TypeReference<List<TeamDataDto>>() {
                        },
                        apiClientObjectMapper)
                .toList();
    }

    private Stream<MatchDto> fetchMatchesByCompetition(Competition competition, LocalDate fromDate) {
        return ResponseHandler.process(
                () -> restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("APIkey", apiKey)
                                .queryParam("action", "get_events")
                                .queryParam("from", fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                                .queryParam("to", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                .queryParam("league_id", competition.getCode())
                                .queryParam("timezone", "UTC")
                                .build())
                        .retrieve()
                        .toEntity(String.class),
                "Failed fetching " + competition + " matches",
                new TypeReference<List<MatchDto>>() {
                },
                apiClientObjectMapper
        );
    }


}

