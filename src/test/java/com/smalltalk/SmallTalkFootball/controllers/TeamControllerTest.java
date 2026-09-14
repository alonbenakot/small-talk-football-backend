package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.TeamSummary;
import com.smalltalk.SmallTalkFootball.models.TeamsResponse;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
}
