package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.CompetitionDataService;
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

@WebMvcTest(controllers = CompetitionDataController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class CompetitionDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetitionDataService service;

    /** The Teams page keys its tabs on the enum name, which the stored document does not hold. */
    @Test
    void carriesTheCompetitionEnumNameNextToTheLeagueId() throws Exception {
        when(service.getCompetitions()).thenReturn(List.of(
                CompetitionData.builder().leagueId(152).leagueName("Premier League").build()));

        mockMvc.perform(get("/competitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].leagueId").value(152))
                .andExpect(jsonPath("$.data[0].competition").value("PREMIER_LEAGUE"));
    }
}
