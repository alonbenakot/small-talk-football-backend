package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.models.dto.TopScorerItem;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import org.springframework.data.mongodb.core.BulkOperations;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
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
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private Mapper<TeamDataDto, List<PlayerData>> playerDataMapper;

    @Captor
    private ArgumentCaptor<Iterable<TeamData>> savedTeams;

    private TeamDataService service;

    @BeforeEach
    void setUp() {
        service = new TeamDataService(repository, mongoTemplate, apiService, standingMapper,
                teamDataUpdateMapper, playerDataMapper);
    }

    @Nested
    class Enrichment {

        /**
         * The match feed can omit team names and coaches (and, for unknown teams, always will),
         * so enrichment fills in whatever the assembled fixture is still missing from stored team data.
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
        void returnsAStoredTeam() throws Exception {
            TeamData team = TestFixtures.teamData("2621", "Liverpool", "Arne Slot");
            when(repository.findById("2621")).thenReturn(Optional.of(team));

            assertThat(service.getTeamById("2621")).isSameAs(team);
        }

        @Test
        void rejectsAnUnknownTeamId() {
            when(repository.findById("nope")).thenReturn(Optional.empty());

            // A 404, not a 500: the team id is user-supplied through the team one-liner route,
            // and ControllerAdvice only maps NotFoundException to a not-found response.
            assertThatThrownBy(() -> service.getTeamById("nope"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("nope");
        }
    }

    @Nested
    class SavingTeams {

        private final ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);

        private final ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);

        private TeamDataDto premierLeagueTeam;

        private BulkOperations bulk;

        @BeforeEach
        void onlyPremierLeagueReturnsData() {
            premierLeagueTeam = mock(TeamDataDto.class);
            when(premierLeagueTeam.getTeamKey()).thenReturn("80");

            when(apiService.getTeamDataList(any())).thenReturn(List.of());
            when(apiService.getTeamDataList(Competition.PREMIER_LEAGUE)).thenReturn(List.of(premierLeagueTeam));
            when(apiService.getTopScorers(any())).thenReturn(List.of());
            when(teamDataUpdateMapper.map(premierLeagueTeam)).thenReturn(new Update());

            bulk = mock(BulkOperations.class);
        }

        /**
         * Only the tests that actually write a squad stub the bulk, so strict stubbing stays
         * useful. The converter is real: the $set document is what the test inspects. The
         * count is lenient because the Champions League case must never reach it.
         */
        private void expectABulkWrite(long storedSquadSize) {
            when(mongoTemplate.getConverter()).thenReturn(converter());
            when(mongoTemplate.bulkOps(any(), eq(PlayerData.class))).thenReturn(bulk);
            lenient().when(mongoTemplate.count(any(), eq(PlayerData.class))).thenReturn(storedSquadSize);
            when(bulk.upsert(any(Query.class), any(Update.class))).thenReturn(bulk);
        }

        /**
         * A bare MongoMappingContext lacks the JSR-310 simple types Boot registers, and would
         * try to map the {@code Instant} inside a cached one-liner as an entity.
         */
        private static MappingMongoConverter converter() {
            MongoCustomConversions conversions = new MongoCustomConversions(List.of());
            MongoMappingContext context = new MongoMappingContext();
            context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
            context.afterPropertiesSet();
            MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
            converter.setCustomConversions(conversions);
            converter.afterPropertiesSet();
            return converter;
        }

        private void squadOf(PlayerData... players) {
            when(playerDataMapper.map(premierLeagueTeam)).thenReturn(List.of(players));
        }

        private static PlayerData player(String id) {
            return PlayerData.builder().id(id).teamId("80").build();
        }

        private Document setOf(Update captured) {
            return captured.getUpdateObject().get("$set", Document.class);
        }

        private Document unsetOf(Update captured) {
            return captured.getUpdateObject().get("$unset", Document.class);
        }

        @Test
        void upsertsTheTeamAndWritesEverySquadMember() {
            expectABulkWrite(2);
            squadOf(player("p1"), player("p2"));

            service.saveCompetitionTeams();

            verify(mongoTemplate).upsert(any(), any(), eq(TeamData.class));
            verify(bulk, times(2)).upsert(any(Query.class), any(Update.class));
            verify(bulk).execute();
        }

        /**
         * A $set of the mapped fields rather than a replace, so whatever the refresh does not
         * write (the cached one-liners, from Phase 2 on) survives it. The id is the upsert key,
         * not a $set field.
         */
        @Test
        void writesEachPlayerAsASetUpsertKeyedOnId() {
            expectABulkWrite(1);
            squadOf(PlayerData.builder().id("p1").teamId("80").name("Erling Haaland").goals(8).build());

            service.saveCompetitionTeams();

            verify(bulk).upsert(query.capture(), update.capture());
            assertThat(query.getValue().getQueryObject()).containsEntry("_id", "p1");
            assertThat(setOf(update.getValue()))
                    .containsEntry("name", "Erling Haaland")
                    .containsEntry("goals", 8)
                    .containsEntry("teamId", "80")
                    .doesNotContainKey("_id");
        }

        /**
         * The converter omits nulls, so $set alone would leave last week's value in place — a
         * scorer rank that dropped out of the charts has to be cleared explicitly.
         */
        @Test
        void clearsAFieldThatWentFromAValueToNull() {
            expectABulkWrite(1);
            squadOf(player("p1"));

            service.saveCompetitionTeams();

            verify(bulk).upsert(any(Query.class), update.capture());
            assertThat(unsetOf(update.getValue()))
                    .containsKey("leagueScorerRank")
                    .containsKey("goals")
                    .doesNotContainKey("_id");
        }

        /**
         * The whole point of the $set write: the cached one-liners are neither set nor unset by
         * a refresh, so a sentence written by the player one-liner service survives it.
         */
        @Test
        void leavesTheCachedOneLinersAloneOnARefresh() {
            expectABulkWrite(1);
            squadOf(player("p1"));

            service.saveCompetitionTeams();

            verify(bulk).upsert(any(Query.class), update.capture());
            assertThat(setOf(update.getValue())).doesNotContainKey("oneLiners");
            assertThat(unsetOf(update.getValue())).doesNotContainKey("oneLiners");
        }

        /**
         * The squad is written in one bulk per team instead of one round-trip per player,
         * which took eleven minutes across a full refresh. A national team comes back with no
         * squad at all, and execute() rejects a bulk holding no operations - so an empty squad
         * has to skip the bulk entirely rather than send an empty one. It must also never be
         * read as "everyone left": the removal below is skipped along with the write.
         */
        @Test
        void writesNothingAndRemovesNobodyForATeamWithNoSquad() {
            squadOf();

            service.saveCompetitionTeams();

            verify(mongoTemplate).upsert(any(), any(), eq(TeamData.class));
            verify(mongoTemplate, never()).bulkOps(any(), eq(PlayerData.class));
            verify(bulk, never()).remove(any(Query.class));
            verify(bulk, never()).execute();
        }

        @Test
        void backfillsTheLeagueScorerRankOntoTheMatchingPlayer() {
            expectABulkWrite(2);
            TopScorerItem scorer = mock(TopScorerItem.class);
            when(scorer.getPlayerKey()).thenReturn("p1");
            when(scorer.getPlayerPlace()).thenReturn("3");
            when(apiService.getTopScorers(Competition.PREMIER_LEAGUE)).thenReturn(List.of(scorer));

            squadOf(player("p1"), player("p2"));

            service.saveCompetitionTeams();

            verify(bulk, times(2)).upsert(any(Query.class), update.capture());
            assertThat(setOf(update.getAllValues().get(0))).containsEntry("leagueScorerRank", 3);
            assertThat(unsetOf(update.getAllValues().get(1))).containsKey("leagueScorerRank");
        }

        /**
         * Bug #11: a player who left the tracked leagues used to keep his teamId forever. The
         * same bulk now removes every stored player of the team who is not in the payload.
         */
        @Test
        void removesStoredPlayersAbsentFromThePayload() {
            expectABulkWrite(3);
            squadOf(player("p1"), player("p2"));

            service.saveCompetitionTeams();

            verify(bulk).remove(query.capture());
            Document removal = query.getValue().getQueryObject();
            assertThat(removal).containsEntry("teamId", "80");
            assertThat(removal.get("_id", Document.class)).containsEntry("$nin", List.of("p1", "p2"));
        }

        /**
         * A payload smaller than half the stored squad is what a truncated response looks like,
         * not a squad that halved between Thursday and Monday. The write still goes ahead; the
         * stale players simply survive until a full response arrives.
         */
        @Test
        void keepsTheStoredSquadWhenThePayloadIsLessThanHalfItsSize() {
            expectABulkWrite(25);
            squadOf(player("p1"), player("p2"));

            service.saveCompetitionTeams();

            verify(bulk, never()).remove(any(Query.class));
            verify(bulk, times(2)).upsert(any(Query.class), any(Update.class));
            verify(bulk).execute();
        }

        /** Two competitions refresh the same club; only the domestic call may remove anyone. */
        @Test
        void removesNobodyFromAChampionsLeagueRefresh() {
            when(apiService.getTeamDataList(Competition.PREMIER_LEAGUE)).thenReturn(List.of());
            when(apiService.getTeamDataList(Competition.CHAMPIONS_LEAGUE)).thenReturn(List.of(premierLeagueTeam));
            expectABulkWrite(3);
            squadOf(player("p1"), player("p2"));

            service.saveCompetitionTeams();

            verify(mongoTemplate, never()).count(any(), eq(PlayerData.class));
            verify(bulk, never()).remove(any(Query.class));
            verify(bulk).execute();
        }
    }
}
