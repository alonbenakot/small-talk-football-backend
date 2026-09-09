package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamDataServiceTest {

    @Mock
    private TeamDataRepository repository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private FootballApiService apiService;
    @Mock
    private Mapper<StandingsDtoItem, Standing> standingMapper;
    @Mock
    private Mapper<TeamDataDto, Update> teamDataUpdateMapper;

    @Captor
    private ArgumentCaptor<Iterable<TeamData>> savedTeams;

    private TeamDataService service;

    @BeforeEach
    void setUp() {
        service = new TeamDataService(repository, mongoTemplate, apiService, standingMapper, teamDataUpdateMapper);
    }

    @Nested
    class Enrichment {

        /**
         * FixtureAssembler cannot read team names or coaches out of the match feed, so every
         * fixture arrives here needing them filled in from stored team data.
         */
        @Test
        void fillsInTheDetailTheMatchFeedDoesNotProvide() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .homeTeam(Team.builder().id(TestFixtures.HOME_TEAM_ID).build())
                    .awayTeam(Team.builder().id(TestFixtures.AWAY_TEAM_ID).build())
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getHomeTeam().getName()).isEqualTo("Liverpool");
            assertThat(fixture.getHomeTeam().getCoach()).isEqualTo("Arne Slot");
            assertThat(fixture.getHomeTeam().getCrest()).isEqualTo("2621-badge.png");
            assertThat(fixture.getAwayTeam().getName()).isEqualTo("Everton");
        }

        @Test
        void doesNotOverwriteDetailTheFixtureAlreadyHas() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .homeTeam(Team.builder()
                            .id(TestFixtures.HOME_TEAM_ID)
                            .name("Nickname FC")
                            .coach("Existing Coach")
                            .crest("existing.png")
                            .build())
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getHomeTeam().getName()).isEqualTo("Nickname FC");
            assertThat(fixture.getHomeTeam().getCoach()).isEqualTo("Existing Coach");
            assertThat(fixture.getHomeTeam().getCrest()).isEqualTo("existing.png");
        }

        @Test
        void leavesUnknownTeamsAlone() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .homeTeam(Team.builder().id("not-a-known-team").build())
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getHomeTeam().getName()).isNull();
        }

        @Test
        void namesTheScoringTeamOnEachGoal() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .goals(new ArrayList<>(List.of(
                            TestFixtures.goal(23, "Salah", null, null, TeamType.HOME, 1, 0),
                            TestFixtures.goal(67, "Calvert-Lewin", null, "  ", TeamType.AWAY, 1, 1))))
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getGoals()).extracting(Goal::getTeamName)
                    .containsExactly("Liverpool", "Everton");
        }

        @Test
        void derivesTheWinnerWhenTheFeedDidNotSupplyOne() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .score(TestFixtures.score(2, 1, null))
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getScore().getWinner()).isEqualTo("Liverpool");
        }

        @Test
        void derivesAnAwayWinner() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .score(TestFixtures.score(0, 2, null))
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getScore().getWinner()).isEqualTo("Everton");
        }

        @Test
        void leavesADrawWithoutAWinner() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .score(TestFixtures.score(1, 1, null))
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getScore().getWinner()).isNull();
        }

        @Test
        void keepsAWinnerTheFeedAlreadySupplied() {
            Fixture fixture = TestFixtures.finishedFixture()
                    .score(TestFixtures.score(2, 1, "Already Set FC"))
                    .build();

            service.enrichTeamsData(fixture, TestFixtures.bothTeamsData());

            assertThat(fixture.getScore().getWinner()).isEqualTo("Already Set FC");
        }
    }

    @Nested
    class Standings {

        @Test
        void storesEachStandingUnderItsCompetition() {
            TeamData team = TestFixtures.teamData(TestFixtures.HOME_TEAM_ID, "Liverpool", "Arne Slot");
            when(repository.findAll()).thenReturn(List.of(team));

            StandingsDtoItem dto = mock(StandingsDtoItem.class);
            when(dto.getTeamId()).thenReturn(TestFixtures.HOME_TEAM_ID);
            when(apiService.getCompetitionStandings(any())).thenReturn(List.of());
            when(apiService.getCompetitionStandings(Competition.PREMIER_LEAGUE)).thenReturn(List.of(dto));

            Standing standing = Standing.builder()
                    .competition(Competition.PREMIER_LEAGUE).position(1).points(46).build();
            when(standingMapper.map(dto)).thenReturn(standing);

            service.refreshStandings();

            assertThat(team.getStandings()).containsEntry(Competition.PREMIER_LEAGUE, standing);
            verify(repository).saveAll(savedTeams.capture());
            assertThat(savedTeams.getValue()).containsExactly(team);
        }

        /*
         * The standings feed covers every team in a league, including ones never stored because
         * they play in no competition the application tracks.
         */
        @Test
        void ignoresStandingsForTeamsThatAreNotStored() {
            when(repository.findAll()).thenReturn(List.of(
                    TestFixtures.teamData(TestFixtures.HOME_TEAM_ID, "Liverpool", "Arne Slot")));

            StandingsDtoItem unknownTeam = mock(StandingsDtoItem.class);
            when(unknownTeam.getTeamId()).thenReturn("9999");
            when(apiService.getCompetitionStandings(any())).thenReturn(List.of(unknownTeam));

            service.refreshStandings();

            verify(standingMapper, never()).map(any());
        }

        @Test
        void queriesEveryTrackedCompetition() {
            when(repository.findAll()).thenReturn(List.of());
            when(apiService.getCompetitionStandings(any())).thenReturn(List.of());

            service.refreshStandings();

            for (Competition competition : Competition.values()) {
                verify(apiService).getCompetitionStandings(competition);
            }
        }
    }

    @Nested
    class Lookup {

        @Test
        void returnsAStoredTeam() {
            TeamData team = TestFixtures.teamData("2621", "Liverpool", "Arne Slot");
            when(repository.findById("2621")).thenReturn(Optional.of(team));

            assertThat(service.getTeamById("2621")).isSameAs(team);
        }

        @Test
        void rejectsAnUnknownTeamId() {
            when(repository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTeamById("nope"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nope");
        }
    }
}
