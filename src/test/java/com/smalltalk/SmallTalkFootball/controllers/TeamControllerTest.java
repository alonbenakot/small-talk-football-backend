package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.TeamFacts;
import com.smalltalk.SmallTalkFootball.models.TeamSummary;
import com.smalltalk.SmallTalkFootball.models.TeamsResponse;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import com.smalltalk.SmallTalkFootball.services.TeamOneLinersService;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TeamController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamDataService service;

    @MockBean
    private TeamOneLinersService oneLinersService;

    @Test
    void returnsTheCompetitionsAndTheTeamsLikeTheFixturesRoute() throws Exception {
        when(service.getTeams()).thenReturn(new TeamsResponse(
                List.of(Competition.PREMIER_LEAGUE),
                List.of(new TeamSummary("141", "Arsenal FC", "arsenal.jpg", Competition.PREMIER_LEAGUE, 1, 12),
                        new TeamSummary("80", "Manchester City", "city.jpg", Competition.PREMIER_LEAGUE, 2, 12))));

        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.competitions[0]").value("PREMIER_LEAGUE"))
                .andExpect(jsonPath("$.data.teams.length()").value(2))
                .andExpect(jsonPath("$.data.teams[0].id").value("141"))
                .andExpect(jsonPath("$.data.teams[0].competition").value("PREMIER_LEAGUE"))
                .andExpect(jsonPath("$.data.teams[0].position").value(1))
                .andExpect(jsonPath("$.data.teams[1].name").value("Manchester City"))
                .andExpect(jsonPath("$.data.teams[1].points").value(12))
                .andExpect(jsonPath("$.data.teams[1].standings").doesNotExist());
    }

    @Test
    void returnsTheFactsBlockWithoutAOneLiner() throws Exception {
        TeamFacts facts = TeamFacts.from(
                TestFixtures.teamDataWithStanding("80", "Manchester City", "Enzo Maresca",
                        Competition.PREMIER_LEAGUE, 2, 12, 5),
                Competition.PREMIER_LEAGUE, List.of(), null, List.of());
        when(oneLinersService.getTeamFacts("80")).thenReturn(facts);

        mockMvc.perform(get("/teams/80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("80"))
                .andExpect(jsonPath("$.data.coach").value("Enzo Maresca"))
                .andExpect(jsonPath("$.data.primaryCompetition").value("PREMIER_LEAGUE"))
                .andExpect(jsonPath("$.data.standings.PREMIER_LEAGUE.position").value(2))
                .andExpect(jsonPath("$.data.nextFixture").value(nullValue()))
                .andExpect(jsonPath("$.data.oneLiner").doesNotExist());
    }

    @Test
    void anUnknownTeamIsANotFoundWithTheSharedMessage() throws Exception {
        when(oneLinersService.getTeamFacts("nope"))
                .thenThrow(new NotFoundException(Messages.NO_TEAM_FOUND.formatted("nope")));

        mockMvc.perform(get("/teams/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.systemMessage.messageText").value("No team was found for id: nope"));
    }
}
