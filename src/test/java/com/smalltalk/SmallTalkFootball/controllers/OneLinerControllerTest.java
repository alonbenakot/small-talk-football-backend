package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.models.PlayerFacts;
import com.smalltalk.SmallTalkFootball.models.PlayerOneLiner;
import com.smalltalk.SmallTalkFootball.models.PlayerSmallTalk;
import com.smalltalk.SmallTalkFootball.models.TeamFacts;
import com.smalltalk.SmallTalkFootball.models.TeamOneLiner;
import com.smalltalk.SmallTalkFootball.models.TeamSmallTalk;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.OneLinersService;
import com.smalltalk.SmallTalkFootball.services.PlayerOneLinersService;
import com.smalltalk.SmallTalkFootball.services.TeamOneLinersService;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OneLinerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class OneLinerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OneLinersService service;

    @MockBean
    private TeamOneLinersService teamService;

    @MockBean
    private PlayerOneLinersService playerService;

    private static OneLiner oneLiner(String text) {
        return OneLiner.builder().teamType(TeamType.HOME).language(Language.BRITISH).text(text).build();
    }

    @Test
    void returnsAOneLinerForTheRequestedTeamAndLanguage() throws Exception {
        when(service.getOneLiner("fixture-1", TeamType.HOME, Language.BRITISH))
                .thenReturn(oneLiner("Cracking result for the Reds."));

        mockMvc.perform(get("/one-liners/fixture-1").param("teamType", "HOME").param("lang", "BRITISH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("Cracking result for the Reds."))
                .andExpect(jsonPath("$.data.teamType").value("HOME"))
                .andExpect(jsonPath("$.data.language").value("BRITISH"));
    }

    @Test
    void allowsTheTeamToBeOmitted() throws Exception {
        when(service.getOneLiner(any(), any(), any())).thenReturn(oneLiner("A neutral take."));

        mockMvc.perform(get("/one-liners/fixture-1").param("lang", "AMERICAN"))
                .andExpect(status().isOk());

        verify(service).getOneLiner("fixture-1", null, Language.AMERICAN);
    }

    @Test
    void requiresTheLanguage() throws Exception {
        mockMvc.perform(get("/one-liners/fixture-1").param("teamType", "HOME"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUnknownLanguage() throws Exception {
        mockMvc.perform(get("/one-liners/fixture-1").param("lang", "KLINGON"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsAnUnknownFixtureAsABadRequest() throws Exception {
        when(service.getOneLiner(any(), any(), any()))
                .thenThrow(new SmallTalkException("Invalid fixture id"));

        mockMvc.perform(get("/one-liners/nope").param("lang", "BRITISH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.systemMessage.messageText").value("Invalid fixture id"));
    }

    /**
     * {@code /one-liners/teams/{id}} has one more path segment than the fixture route, so the
     * two never collide — worth pinning, since a single-segment team id would otherwise be
     * routed to the fixture handler.
     */
    @Nested
    class TeamRoute {

        private TeamSmallTalk smallTalk(String text) {
            TeamOneLiner oneLiner = TeamOneLiner.builder()
                    .language(Language.BRITISH)
                    .competition(Competition.PREMIER_LEAGUE)
                    .perspective(Perspective.FAN)
                    .text(text)
                    .generatedAt(Instant.parse("2026-09-09T10:02:11Z"))
                    .positionAtGeneration(1)
                    .pointsAtGeneration(13)
                    .build();

            TeamFacts facts = TeamFacts.builder()
                    .id("2611")
                    .name("Arsenal")
                    .coach("Mikel Arteta")
                    .primaryCompetition(Competition.PREMIER_LEAGUE)
                    .build();

            return new TeamSmallTalk(oneLiner, facts);
        }

        @Test
        void returnsTheSentenceAndTheFacts() throws Exception {
            when(teamService.getTeamSmallTalk("2611", null, Language.BRITISH, Perspective.FAN))
                    .thenReturn(smallTalk("Four on the bounce and top of the table."));

            mockMvc.perform(get("/one-liners/teams/2611")
                            .param("lang", "BRITISH")
                            .param("perspective", "FAN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.oneLiner.text").value("Four on the bounce and top of the table."))
                    .andExpect(jsonPath("$.data.oneLiner.perspective").value("FAN"))
                    .andExpect(jsonPath("$.data.facts.name").value("Arsenal"))
                    .andExpect(jsonPath("$.data.facts.primaryCompetition").value("PREMIER_LEAGUE"));
        }

        /** The standings snapshot is a caching detail, not something the client should see. */
        @Test
        void doesNotLeakTheStandingsSnapshot() throws Exception {
            when(teamService.getTeamSmallTalk(any(), any(), any(), any()))
                    .thenReturn(smallTalk("Arsenal are top on thirteen points."));

            mockMvc.perform(get("/one-liners/teams/2611").param("lang", "BRITISH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.oneLiner.positionAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.oneLiner.pointsAtGeneration").doesNotExist());
        }

        @Test
        void defaultsToTheNeutralPerspectiveAndNoCompetition() throws Exception {
            when(teamService.getTeamSmallTalk(any(), any(), any(), any()))
                    .thenReturn(smallTalk("Arsenal are top on thirteen points."));

            mockMvc.perform(get("/one-liners/teams/2611").param("lang", "BRITISH"))
                    .andExpect(status().isOk());

            verify(teamService).getTeamSmallTalk("2611", null, Language.BRITISH, Perspective.NEUTRAL);
        }

        @Test
        void passesTheRequestedCompetitionThrough() throws Exception {
            when(teamService.getTeamSmallTalk(any(), any(), any(), any()))
                    .thenReturn(smallTalk("Second in the group."));

            mockMvc.perform(get("/one-liners/teams/2611")
                            .param("lang", "BRITISH")
                            .param("competition", "CHAMPIONS_LEAGUE"))
                    .andExpect(status().isOk());

            verify(teamService).getTeamSmallTalk(
                    "2611", Competition.CHAMPIONS_LEAGUE, Language.BRITISH, Perspective.NEUTRAL);
        }

        @Test
        void requiresTheLanguage() throws Exception {
            mockMvc.perform(get("/one-liners/teams/2611").param("perspective", "FAN"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsAnUnknownPerspective() throws Exception {
            mockMvc.perform(get("/one-liners/teams/2611").param("lang", "BRITISH").param("perspective", "HOOLIGAN"))
                    .andExpect(status().isBadRequest());
        }

        /** A national side has no league position, so there is nothing honest to say (§6.5). */
        @Test
        void reportsAWorldCupOnlyTeamAsABadRequest() throws Exception {
            when(teamService.getTeamSmallTalk(any(), any(), any(), any()))
                    .thenThrow(new SmallTalkException(Messages.TEAM_HAS_NO_LEAGUE_STANDING));

            mockMvc.perform(get("/one-liners/teams/england").param("lang", "BRITISH"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.systemMessage.messageText")
                            .value(Messages.TEAM_HAS_NO_LEAGUE_STANDING));
        }

        @Test
        void reportsAnUnknownTeamAsANotFound() throws Exception {
            when(teamService.getTeamSmallTalk(any(), any(), any(), any()))
                    .thenThrow(new NotFoundException(Messages.NO_TEAM_FOUND.formatted("nope")));

            mockMvc.perform(get("/one-liners/teams/nope").param("lang", "BRITISH"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.systemMessage.messageText")
                            .value(Messages.NO_TEAM_FOUND.formatted("nope")));
        }
    }

    @Nested
    class PlayerRoute {

        private PlayerSmallTalk smallTalk(String text) {
            PlayerOneLiner oneLiner = PlayerOneLiner.builder()
                    .language(Language.BRITISH)
                    .text(text)
                    .generatedAt(Instant.parse("2026-09-10T10:02:11Z"))
                    .matchesPlayedAtGeneration(10)
                    .goalsAtGeneration(8)
                    .assistsAtGeneration(0)
                    .injuredAtGeneration(false)
                    .scorerRankAtGeneration(1)
                    .build();

            PlayerFacts facts = new PlayerFacts("659972248", "Erling Haaland", null, "9", "Forwards", "26",
                    true, false, new PlayerFacts.Club("80", "Manchester City", null, "Enzo Maresca"),
                    Competition.PREMIER_LEAGUE,
                    new PlayerFacts.Season(10, 8, null, null, null, null, null, null, null, null, null, null,
                            null, null, "7.30", null, null, null),
                    1, null, null, null);

            return new PlayerSmallTalk(oneLiner, facts);
        }

        @Test
        void returnsTheSentenceAndTheFacts() throws Exception {
            when(playerService.getPlayerSmallTalk("659972248", Language.BRITISH))
                    .thenReturn(smallTalk("Haaland's eight in ten and top of the charts."));

            mockMvc.perform(get("/one-liners/players/659972248").param("lang", "BRITISH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.oneLiner.text").value("Haaland's eight in ten and top of the charts."))
                    .andExpect(jsonPath("$.data.oneLiner.language").value("BRITISH"))
                    .andExpect(jsonPath("$.data.facts.name").value("Erling Haaland"))
                    .andExpect(jsonPath("$.data.facts.team.name").value("Manchester City"))
                    .andExpect(jsonPath("$.data.facts.season.goals").value(8))
                    .andExpect(jsonPath("$.data.facts.leagueScorerRank").value(1));
        }

        /** The snapshot is a caching detail, not something the client should see; nulls in season stay null. */
        @Test
        void doesNotLeakTheSnapshotAndKeepsAbsentStatsNull() throws Exception {
            when(playerService.getPlayerSmallTalk(any(), any())).thenReturn(smallTalk("Eight in ten."));

            mockMvc.perform(get("/one-liners/players/659972248").param("lang", "BRITISH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.oneLiner.matchesPlayedAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.oneLiner.goalsAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.oneLiner.assistsAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.oneLiner.injuredAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.oneLiner.scorerRankAtGeneration").doesNotExist())
                    .andExpect(jsonPath("$.data.facts.season.assists").value((Object) null));
        }

        @Test
        void requiresTheLanguage() throws Exception {
            mockMvc.perform(get("/one-liners/players/659972248"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void reportsAnUnknownPlayerAsANotFound() throws Exception {
            when(playerService.getPlayerSmallTalk(any(), any()))
                    .thenThrow(new NotFoundException(Messages.NO_PLAYER_FOUND.formatted("nope")));

            mockMvc.perform(get("/one-liners/players/nope").param("lang", "BRITISH"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.systemMessage.messageText")
                            .value(Messages.NO_PLAYER_FOUND.formatted("nope")));
        }
    }
}
