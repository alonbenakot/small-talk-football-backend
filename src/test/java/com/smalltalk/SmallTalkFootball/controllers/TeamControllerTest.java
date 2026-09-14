package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.TeamSummary;
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
    void listsACompetitionsTeamsInTheOrderTheServiceGaveThem() throws Exception {
        when(service.getTeamsByCompetition(Competition.PREMIER_LEAGUE)).thenReturn(List.of(
                new TeamSummary("141", "Arsenal FC", "arsenal.jpg", 1, 12),
                new TeamSummary("80", "Manchester City", "city.jpg", 2, 12)));

        mockMvc.perform(get("/teams").param("competition", "PREMIER_LEAGUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("141"))
                .andExpect(jsonPath("$.data[0].position").value(1))
                .andExpect(jsonPath("$.data[1].name").value("Manchester City"))
                .andExpect(jsonPath("$.data[1].points").value(12))
                .andExpect(jsonPath("$.data[1].standings").doesNotExist());
    }

    @Test
    void requiresTheCompetition() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isBadRequest());
    }
}
