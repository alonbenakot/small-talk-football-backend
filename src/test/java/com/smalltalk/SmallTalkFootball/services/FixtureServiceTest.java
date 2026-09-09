package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * The ingestion rules that keep a re-fetch from corrupting stored fixtures: finished matches
 * are never overwritten, re-fetched matches keep their Mongo id, and an empty API response
 * must not be mistaken for "there are no fixtures".
 */
@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    private static final int MATCH_DAYS = 7;
    private static final int DAYS_AHEAD = 3;

    @Mock
    private FootballApiService footballApiService;
    @Mock
    private TeamDataService teamDataService;
    @Mock
    private FixtureRepository repository;
    @Mock
    private Mapper<MatchDto, Fixture> fullFixtureMapper;

    @Captor
    private ArgumentCaptor<List<Fixture>> savedFixtures;

    private FixtureService service;

    @BeforeEach
    void setUp() {
        service = new FixtureService(footballApiService, teamDataService, repository, fullFixtureMapper);
        // Lenient: most tests do not care about enrichment, but the service always consults it.
        lenient().when(teamDataService.getTeamsData()).thenReturn(TestFixtures.bothTeamsData());
        lenient().when(teamDataService.enrichTeamsData(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Wires the API to return one match per fixture, which the mapper turns into that fixture.
     */
    private void apiReturns(Fixture... fixtures) {
        List<MatchDto> dtos = Arrays.stream(fixtures)
                .map(fixture -> mock(MatchDto.class))
                .toList();

        when(footballApiService.getMatches(any(), any())).thenReturn(dtos);
        for (int i = 0; i < dtos.size(); i++) {
            when(fullFixtureMapper.map(dtos.get(i))).thenReturn(fixtures[i]);
        }
    }

    private void storedFixtures(Fixture... fixtures) {
        when(repository.findByMatchDateTimeAfter(any())).thenReturn(List.of(fixtures));
    }

    @Test
    void savesFixturesReturnedByTheApi() {
        Fixture incoming = TestFixtures.upcomingFixture().id(null).externalId(9001).build();
        apiReturns(incoming);
        storedFixtures();

        List<Fixture> saved = service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        assertThat(saved).containsExactly(incoming);
        verify(repository).saveAll(savedFixtures.capture());
        assertThat(savedFixtures.getValue()).containsExactly(incoming);
    }

    /*
     * An apifootball outage returns an empty list. Pruning on that would silently wipe every
     * upcoming fixture, so the service leaves the collection alone.
     */
    @Test
    void doesNotPruneOrSaveWhenTheApiReturnsNothing() {
        when(footballApiService.getMatches(any(), any())).thenReturn(List.of());
        storedFixtures();

        List<Fixture> saved = service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(anyList());
        verify(repository, never()).deleteByMatchDateTimeBefore(any());
    }

    @Test
    void skipsMatchesAlreadyStoredAsFinished() {
        Fixture storedFinished = TestFixtures.finishedFixture().id("stored-1").externalId(9001).build();
        Fixture refetched = TestFixtures.finishedFixture().id(null).externalId(9001).build();
        apiReturns(refetched);
        storedFixtures(storedFinished);

        List<Fixture> saved = service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void refreshesMatchesThatAreStoredButNotYetFinished() {
        Fixture storedUnfinished = TestFixtures.upcomingFixture().id("stored-1").externalId(9001).build();
        Fixture refetched = TestFixtures.finishedFixture().id(null).externalId(9001).build();
        apiReturns(refetched);
        storedFixtures(storedUnfinished);

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        verify(repository).saveAll(savedFixtures.capture());
        assertThat(savedFixtures.getValue()).containsExactly(refetched);
    }

    /**
     * Without this the re-fetched match would be inserted as a second document rather than
     * updating the existing one.
     */
    @Test
    void carriesTheExistingMongoIdOntoTheRefetchedFixture() {
        Fixture storedUnfinished = TestFixtures.upcomingFixture().id("stored-1").externalId(9001).build();
        Fixture refetched = TestFixtures.finishedFixture().id(null).externalId(9001).build();
        apiReturns(refetched);
        storedFixtures(storedUnfinished);

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        assertThat(refetched.getId()).isEqualTo("stored-1");
    }

    @Test
    void leavesBrandNewFixturesWithoutAnId() {
        Fixture incoming = TestFixtures.upcomingFixture().id(null).externalId(9002).build();
        apiReturns(incoming);
        storedFixtures(TestFixtures.upcomingFixture().id("stored-1").externalId(9001).build());

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        assertThat(incoming.getId()).isNull();
    }

    @Test
    void prunesFixturesOlderThanTheStartOfTheWindow() {
        apiReturns(TestFixtures.upcomingFixture().id(null).externalId(9001).build());
        storedFixtures();

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        Instant expectedCutoff = LocalDate.now().minusDays(MATCH_DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        verify(repository).deleteByMatchDateTimeBefore(expectedCutoff);
    }

    @Test
    void asksTheApiForTheConfiguredWindow() {
        when(footballApiService.getMatches(any(), any())).thenReturn(List.of());
        storedFixtures();

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        verify(footballApiService).getMatches(
                LocalDate.now().minusDays(MATCH_DAYS),
                LocalDate.now().plusDays(DAYS_AHEAD));
    }

    @Test
    void enrichesEveryFixtureWithStoredTeamData() {
        List<TeamData> teams = TestFixtures.bothTeamsData();
        lenient().when(teamDataService.getTeamsData()).thenReturn(teams);
        Fixture incoming = TestFixtures.upcomingFixture().id(null).externalId(9001).build();
        apiReturns(incoming);
        storedFixtures();

        service.fetchAndSaveFixtures(MATCH_DAYS, DAYS_AHEAD);

        verify(teamDataService).enrichTeamsData(incoming, teams);
    }

    @Test
    void listsTheDistinctCompetitionsThatHaveFixtures() {
        when(repository.findAll()).thenReturn(new ArrayList<>(List.of(
                TestFixtures.finishedFixture().competition(Competition.PREMIER_LEAGUE).build(),
                TestFixtures.finishedFixture().competition(Competition.CHAMPIONS_LEAGUE).build(),
                TestFixtures.finishedFixture().competition(Competition.PREMIER_LEAGUE).build())));

        FixturesResponse response = service.getFixtures();

        assertThat(response.getCompetitions())
                .containsExactly(Competition.PREMIER_LEAGUE, Competition.CHAMPIONS_LEAGUE);
        assertThat(response.getFixtures()).hasSize(3);
    }

    @Test
    void ordersFixturesByCompetition() {
        when(repository.findAll()).thenReturn(new ArrayList<>(List.of(
                TestFixtures.finishedFixture().competition(Competition.SERIA_A).build(),
                TestFixtures.finishedFixture().competition(Competition.WORLD_CUP).build(),
                TestFixtures.finishedFixture().competition(Competition.PREMIER_LEAGUE).build())));

        FixturesResponse response = service.getFixtures();

        assertThat(response.getFixtures()).extracting(Fixture::getCompetition)
                .containsExactly(Competition.WORLD_CUP, Competition.PREMIER_LEAGUE, Competition.SERIA_A);
    }

    @Test
    void returnsAStoredFixtureById() throws SmallTalkException {
        Fixture stored = TestFixtures.finishedFixture().build();
        when(repository.findById("fixture-1")).thenReturn(Optional.of(stored));

        assertThat(service.getFixture("fixture-1")).isSameAs(stored);
    }

    @Test
    void rejectsAnUnknownFixtureId() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFixture("nope"))
                .isInstanceOf(SmallTalkException.class)
                .hasMessage("Invalid fixture id");
    }

    @Test
    void deletingAllFixturesClearsTheCollection() {
        service.deleteAllFixtures();

        verify(repository).deleteAll();
    }
}
