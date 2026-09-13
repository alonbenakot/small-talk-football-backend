package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.models.PlayerSummary;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.PlayerDataService;
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

@WebMvcTest(controllers = PlayerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerDataService service;

    @Test
    void listsTheStoredSquadForATeam() throws Exception {
        when(service.getSquadSummaries("2611")).thenReturn(List.of(
                new PlayerSummary("p1", "David Raya", "raya.jpg", "1", "Goalkeepers", false, 10),
                new PlayerSummary("p2", "Bukayo Saka", "saka.jpg", "7", "Forwards", true, 4)));

        mockMvc.perform(get("/players/teams/2611"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("p1"))
                .andExpect(jsonPath("$.data[0].name").value("David Raya"))
                .andExpect(jsonPath("$.data[0].position").value("Goalkeepers"))
                .andExpect(jsonPath("$.data[1].injured").value(true))
                .andExpect(jsonPath("$.data[1].matchesPlayed").value(4))
                .andExpect(jsonPath("$.data[1].goals").doesNotExist());
    }

    @Test
    void returnsAnEmptyListForATeamWithNoStoredSquad() throws Exception {
        when(service.getSquadSummaries("nope")).thenReturn(List.of());

        mockMvc.perform(get("/players/teams/nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
