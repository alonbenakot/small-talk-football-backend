package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.security.JwtAuthFilter;
import com.smalltalk.SmallTalkFootball.services.FixtureService;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authentication is covered exhaustively in JwtAuthFilterTest, so the filter is excluded here
 * to keep these focused on the controller contract.
 */
@WebMvcTest(controllers = FixtureController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
class FixtureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FixtureService service;

    @Test
    void wrapsFixturesInTheResponseEnvelope() throws Exception {
        Fixture fixture = TestFixtures.finishedFixture().build();
        when(service.getFixtures()).thenReturn(
                new FixturesResponse(List.of(Competition.PREMIER_LEAGUE), List.of(fixture)));

        mockMvc.perform(get("/fixtures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.competitions[0]").value("PREMIER_LEAGUE"))
                .andExpect(jsonPath("$.data.fixtures[0].venue").value("Anfield"))
                .andExpect(jsonPath("$.data.fixtures[0].homeTeam.name").value("Liverpool"));
    }

    @Test
    void returnsASingleFixture() throws Exception {
        when(service.getFixture("fixture-1")).thenReturn(TestFixtures.finishedFixture().build());

        mockMvc.perform(get("/fixtures/fixture-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalId").value(9001));
    }

    @Test
    void reportsAnUnknownFixtureAsABadRequest() throws Exception {
        when(service.getFixture("nope")).thenThrow(new SmallTalkException("Invalid fixture id"));

        mockMvc.perform(get("/fixtures/nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.systemMessage.messageText").value("Invalid fixture id"))
                .andExpect(jsonPath("$.systemMessage.error").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /*
     * Pins current behaviour. The @Min/@Max constraints on the request parameters are enforced,
     * but ControllerAdvice does not handle ConstraintViolationException, so nothing turns it
     * into a 400 with an explanation - in production it falls through to a bare 500. Under
     * MockMvc there is no container error page, so the exception simply escapes perform().
     */
    @Test
    void enforcesTheWindowBoundsWithoutTranslatingTheFailure() {
        assertThatThrownBy(() -> mockMvc.perform(post("/fixtures").param("matchDays", "-1")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("must be greater than or equal to 0");

        assertThatThrownBy(() -> mockMvc.perform(post("/fixtures").param("matchDays", "400")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("must be less than or equal to 365");

        assertThatThrownBy(() -> mockMvc.perform(
                post("/fixtures").param("matchDays", "5").param("matchDaysIntoFuture", "0")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void requiresTheMatchDaysParameter() throws Exception {
        mockMvc.perform(post("/fixtures"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void triggersAFetchWithTheRequestedWindow() throws Exception {
        when(service.fetchAndSaveFixtures(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(post("/fixtures").param("matchDays", "5").param("matchDaysIntoFuture", "10"))
                .andExpect(status().isCreated());

        verify(service).fetchAndSaveFixtures(5, 10);
    }

    @Test
    void defaultsTheFutureWindowToAWeek() throws Exception {
        when(service.fetchAndSaveFixtures(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(post("/fixtures").param("matchDays", "5"))
                .andExpect(status().isCreated());

        verify(service).fetchAndSaveFixtures(5, 7);
    }

    @Test
    void deletesEveryFixture() throws Exception {
        mockMvc.perform(delete("/fixtures")).andExpect(status().isOk());

        verify(service).deleteAllFixtures();
    }
}
