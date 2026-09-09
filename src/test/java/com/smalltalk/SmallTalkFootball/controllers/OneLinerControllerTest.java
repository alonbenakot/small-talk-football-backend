package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.OneLinersService;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

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
}
