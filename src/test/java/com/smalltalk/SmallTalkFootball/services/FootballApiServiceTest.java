package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.config.ObjectMapperConfig;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercises the apifootball.com boundary against a canned server: the query the service
 * builds, the snake_case binding on the way back, and the guarantee that a bad response
 * degrades to an empty result instead of an exception.
 */
class FootballApiServiceTest {

    private static final String API_KEY = "test-api-football-key";

    private MockRestServiceServer server;
    private FootballApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new FootballApiService(builder, API_KEY, new ObjectMapperConfig().apiClientObjectMapper());
    }

    private void respondWith(String body) {
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("apifootball.com")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void respondWith(HttpStatus status, String body) {
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("apifootball.com")))
                .andRespond(withStatus(status).body(body).contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void sendsTheApiKeyAndActionForStandings() {
        server.expect(requestTo(containsString("APIkey=" + API_KEY)))
                .andExpect(requestTo(containsString("action=get_standings")))
                .andExpect(requestTo(containsString("league_id=152")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        service.getCompetitionStandings(Competition.PREMIER_LEAGUE);

        server.verify();
    }

    @Test
    void bindsStandingsFieldNames() {
        respondWith("""
                [{"league_id":"152","team_id":"2621","team_name":"Liverpool",
                  "overall_league_position":"1","overall_league_payed":"20",
                  "overall_league_W":"14","overall_league_D":"4","overall_league_L":"2"}]
                """);

        List<StandingsDtoItem> standings = service.getCompetitionStandings(Competition.PREMIER_LEAGUE);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).getTeamName()).isEqualTo("Liverpool");
        assertThat(standings.get(0).getOverallLeagueW()).isEqualTo("14");
        assertThat(standings.get(0).getOverallLeaguePosition()).isEqualTo("1");
    }

    @Test
    void bindsTeamFieldNames() {
        respondWith("""
                [{"team_key":"2621","team_name":"Liverpool","team_badge":"liverpool.png",
                  "coaches":[{"coach_name":"Arne Slot"}]}]
                """);

        List<TeamDataDto> teams = service.getTeamDataList(Competition.PREMIER_LEAGUE);

        assertThat(teams).hasSize(1);
        assertThat(teams.get(0).getTeamKey()).isEqualTo("2621");
        assertThat(teams.get(0).getCoaches().get(0).getCoachName()).isEqualTo("Arne Slot");
    }

    @Test
    void bindsCompetitionFieldNames() {
        respondWith("""
                [{"league_id":"152","league_name":"Premier League","league_logo":"pl.png",
                  "league_season":"2025/2026","country_name":"England"}]
                """);

        List<CompetitionDto> competitions = service.getCompetitionData();

        assertThat(competitions).hasSize(1);
        assertThat(competitions.get(0).getLeagueName()).isEqualTo("Premier League");
        assertThat(competitions.get(0).getLeagueSeason()).isEqualTo("2025/2026");
    }

    @Test
    void bindsHeadToHeadFieldNames() {
        respondWith("""
                {"firstTeam_lastResults":[{"match_id":"1","match_hometeam_name":"Liverpool"}],
                 "secondTeam_lastResults":[],
                 "firstTeam_VS_secondTeam":[{"match_id":"2","match_hometeam_name":"Everton"}]}
                """);

        Optional<HeadToHeadResponse> response = service.getHeadToHeadData("2621", "2622");

        assertThat(response).isPresent();
        assertThat(response.get().getFirstTeamLastResults()).hasSize(1);
        assertThat(response.get().getFirstTeamVSSecondTeam()).hasSize(1);
        assertThat(response.get().getSecondTeamLastResults()).isEmpty();
    }

    @Test
    void asksForMatchesInIsoDateRangeAcrossEveryCompetition() {
        server.expect(ExpectedCount.times(Competition.values().length),
                        requestTo(containsString("action=get_events")))
                .andExpect(requestTo(containsString("from=2026-03-01")))
                .andExpect(requestTo(containsString("to=2026-03-08")))
                .andExpect(requestTo(containsString("timezone=UTC")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        service.getMatches(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8));

        server.verify();
    }

    @Test
    void aggregatesMatchesFromEveryCompetition() {
        respondWith("""
                [{"match_id":"9001","league_id":"152","match_date":"2026-03-01",
                  "match_time":"20:45","match_status":"Finished"}]
                """);

        List<MatchDto> matches = service.getMatches(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8));

        assertThat(matches).hasSize(Competition.values().length);
    }

    @Test
    void returnsAnEmptyListWhenTheApiFails() {
        respondWith(HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\":\"boom\"}");

        assertThat(service.getCompetitionStandings(Competition.PREMIER_LEAGUE)).isEmpty();
    }

    @Test
    void returnsAnEmptyListWhenTheApiAnswersWithAnHtmlErrorPage() {
        respondWith("<!DOCTYPE html><html><body>Service Unavailable</body></html>");

        assertThat(service.getTeamDataList(Competition.PREMIER_LEAGUE)).isEmpty();
        assertThat(service.getCompetitionData()).isEmpty();
    }

    @Test
    void returnsAnEmptyOptionalWhenHeadToHeadDataIsUnavailable() {
        respondWith(HttpStatus.NOT_FOUND, "{}");

        assertThat(service.getHeadToHeadData("2621", "2622")).isEmpty();
    }

    @Test
    void survivesAMalformedBody() {
        respondWith("{ this is not json");

        assertThat(service.getCompetitionStandings(Competition.PREMIER_LEAGUE)).isEmpty();
    }

    /**
     * One failing competition must not lose the matches fetched for the others.
     */
    @Test
    void keepsMatchesFromCompetitionsThatSucceededWhenOneFails() {
        server.expect(ExpectedCount.once(), requestTo(containsString("league_id=28")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));
        server.expect(ExpectedCount.manyTimes(), requestTo(containsString("action=get_events")))
                .andRespond(withSuccess("""
                        [{"match_id":"9001","league_id":"152"}]
                        """, MediaType.APPLICATION_JSON));

        List<MatchDto> matches = service.getMatches(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8));

        assertThat(matches).hasSize(Competition.values().length - 1);
    }
}
