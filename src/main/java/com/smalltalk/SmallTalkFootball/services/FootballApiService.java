package com.smalltalk.SmallTalkFootball.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.system.utils.ResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
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

    private final ObjectMapper objectMapper;

    public FootballApiService(RestClient.Builder builder, @Value("${api.football.key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    public List<MatchDto> getMatches(LocalDate fromDate) {
        return Arrays.stream(Competition.values())
                .flatMap(competition -> fetchMatchesByCompetition(competition, fromDate))
                .toList();
    }

    public List<TeamDataDto> getTeamData(Competition competition) {
        return restClient.get().uri(uriBuilder -> uriBuilder
                        .queryParam("APIkey", apiKey)
                        .queryParam("action", "get_teams")
                        .queryParam("league_id", competition.getCode())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private Stream<MatchDto> fetchMatchesByCompetition(Competition competition, LocalDate fromDate) {
        ResponseEntity<String> response = null;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("APIkey", apiKey)
                            .queryParam("action", "get_events")
                            .queryParam("from", fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("to", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("league_id", competition.getCode())
                            .build())
                    .retrieve()
                    .toEntity(String.class);

            if (ResponseHandler.isValidResponse(response)) {
                List<MatchDto> matches = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<>() {
                        }
                );
                return matches.stream();
            } else {
                ResponseHandler.logResponseError(response, "Failed fetching " + competition + " matches");
                return Stream.empty();
            }
        } catch (Exception e) {
            ResponseHandler.logResponseError(response, "Failed fetching " + competition + " matches");
            return Stream.empty();
        }
    }


}

