package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.models.dto.CompetitionDto;
import com.smalltalk.SmallTalkFootball.repositories.CompetitionDataRepository;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionDataServiceTest {

    @Mock
    private CompetitionDataRepository repository;
    @Mock
    private FootballApiService apiService;

    private CompetitionDataService service;

    @BeforeEach
    void setUp() {
        service = new CompetitionDataService(repository, apiService);
    }

    private static CompetitionDto dto(String leagueId, String name) {
        return JsonFixtures.parse("""
                {"league_id":"%s","league_name":"%s","league_logo":"logo.png",
                 "league_season":"2025/2026","country_name":"England",
                 "country_id":"41","country_logo":"england.png"}
                """.formatted(leagueId, name), CompetitionDto.class);
    }

    @Test
    void keepsOnlyCompetitionsTheApplicationTracks() {
        when(apiService.getCompetitionData()).thenReturn(List.of(
                dto("152", "Premier League"),
                dto("999", "Some Other League"),
                dto("3", "Champions League")));

        List<CompetitionData> saved = service.fetchAndSaveCompetitions();

        assertThat(saved).extracting(CompetitionData::getLeagueName)
                .containsExactly("Premier League", "Champions League");
    }

    @Test
    void replacesTheStoredCompetitions() {
        when(apiService.getCompetitionData()).thenReturn(List.of(dto("152", "Premier League")));

        service.fetchAndSaveCompetitions();

        verify(repository).deleteAll();
        verify(repository).saveAll(any());
    }

    /*
     * Guards against an apifootball outage emptying the collection: with nothing to store,
     * the existing competitions are left in place.
     */
    @Test
    void leavesStoredCompetitionsAloneWhenTheApiReturnsNothing() {
        when(apiService.getCompetitionData()).thenReturn(List.of());

        assertThat(service.fetchAndSaveCompetitions()).isEmpty();

        verify(repository, never()).deleteAll();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void leavesStoredCompetitionsAloneWhenNothingMatchesTheWhitelist() {
        when(apiService.getCompetitionData()).thenReturn(List.of(dto("999", "Some Other League")));

        assertThat(service.fetchAndSaveCompetitions()).isEmpty();

        verify(repository, never()).deleteAll();
    }

    /*
     * The league id is parsed before it is checked, so a non-numeric one aborts the refresh
     * rather than being filtered out like an unknown-but-numeric id.
     */
    @Test
    void throwsOnANonNumericLeagueId() {
        when(apiService.getCompetitionData()).thenReturn(List.of(dto("not-a-number", "Broken League")));

        assertThatThrownBy(() -> service.fetchAndSaveCompetitions())
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void readsBackTheStoredCompetitions() {
        List<CompetitionData> stored = List.of(CompetitionData.builder().leagueId(152).build());
        when(repository.findAll()).thenReturn(stored);

        assertThat(service.getCompetitions()).isSameAs(stored);
    }
}
