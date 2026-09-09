package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.models.TeamFacts;
import com.smalltalk.SmallTalkFootball.models.TeamOneLiner;
import com.smalltalk.SmallTalkFootball.models.TeamSmallTalk;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilder;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilderFactory;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.TeamPromptContext;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * The team one-liner has no TTL: a cached sentence survives until the team plays again or the
 * table moves under it (plan §5). Both halves of that rule are pinned here, along with the
 * replaceOneLiner trap — TeamOneLiner equality ignores its text, so a regeneration written
 * with add() would be a silent no-op and the stale sentence would live forever.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamOneLinersServiceTest {

    private static final String TEAM_ID = TestFixtures.HOME_TEAM_ID;
    private static final Instant GENERATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Mock
    private TeamDataService teamDataService;

    @Mock
    private FixtureService fixtureService;

    @Mock
    private PlayerDataService playerDataService;

    @Mock
    private AiService aiService;

    @Mock
    private PromptBuilderFactory promptBuilderFactory;

    @Mock
    private PromptBuilder promptBuilder;

    private TeamOneLinersService service;

    @BeforeEach
    void setUp() {
        service = new TeamOneLinersService(
                teamDataService, fixtureService, playerDataService, aiService, promptBuilderFactory);

        when(promptBuilderFactory.create(any(TeamPromptContext.class), any(), any())).thenReturn(promptBuilder);
        when(promptBuilder.buildPrompt()).thenReturn("the prompt");
        when(aiService.generate(any())).thenReturn("a freshly generated sentence");
        when(fixtureService.getRecentFinishedForTeam(any(), anyInt())).thenReturn(List.of());
        when(fixtureService.getNextFixtureForTeam(any())).thenReturn(Optional.empty());
        when(playerDataService.getNotablePlayers(any())).thenReturn(List.of());
    }

    /** A team sitting first on 13 points, with one cached British-English fan sentence. */
    private TeamData teamWithCachedOneLiner(String text) {
        TeamData team = TestFixtures.teamDataWithStanding(
                TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5);
        team.addOneLiner(cached(text, 1, 13));
        return team;
    }

    private static TeamOneLiner cached(String text, Integer position, Integer points) {
        return TeamOneLiner.builder()
                .language(Language.BRITISH)
                .competition(Competition.PREMIER_LEAGUE)
                .perspective(Perspective.FAN)
                .text(text)
                .generatedAt(GENERATED_AT)
                .positionAtGeneration(position)
                .pointsAtGeneration(points)
                .build();
    }

    private TeamSmallTalk callFan(TeamData team) throws SmallTalkException {
        when(teamDataService.getTeamById(TEAM_ID)).thenReturn(team);
        return service.getTeamSmallTalk(TEAM_ID, null, Language.BRITISH, Perspective.FAN);
    }

    private static Fixture playedAt(Instant when) {
        return TestFixtures.finishedFixture().matchDateTime(when).build();
    }

    @Nested
    class Caching {

        @Test
        void reusesTheCachedSentenceWhenNothingHasChanged() throws Exception {
            when(fixtureService.getRecentFinishedForTeam(any(), anyInt()))
                    .thenReturn(List.of(playedAt(GENERATED_AT.minus(3, ChronoUnit.DAYS))));

            TeamSmallTalk result = callFan(teamWithCachedOneLiner("the cached sentence"));

            assertThat(result.getOneLiner().getText()).isEqualTo("the cached sentence");
            verify(aiService, never()).generate(any());
            verify(teamDataService, never()).save(any());
        }

        @Test
        void regeneratesAfterTheTeamHasPlayedAgain() throws Exception {
            when(fixtureService.getRecentFinishedForTeam(any(), anyInt()))
                    .thenReturn(List.of(playedAt(GENERATED_AT.plus(1, ChronoUnit.DAYS))));

            TeamSmallTalk result = callFan(teamWithCachedOneLiner("the cached sentence"));

            assertThat(result.getOneLiner().getText()).isEqualTo("a freshly generated sentence");
            verify(aiService).generate("the prompt");
        }

        /**
         * A team's position moves when other teams play, so a sentence can go stale during a
         * break in its own fixtures. Without this rule "top of the table" survives being
         * overtaken.
         */
        @Test
        void regeneratesAfterAStandingsMoveWithNoNewFixture() throws Exception {
            TeamData team = teamWithCachedOneLiner("top of the table");
            team.getStandings().put(Competition.PREMIER_LEAGUE,
                    TestFixtures.standing(Competition.PREMIER_LEAGUE, 2, 13, 5));

            assertThat(callFan(team).getOneLiner().getText()).isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesWhenOnlyThePointsMoved() throws Exception {
            TeamData team = teamWithCachedOneLiner("thirteen points");
            team.getStandings().put(Competition.PREMIER_LEAGUE,
                    TestFixtures.standing(Competition.PREMIER_LEAGUE, 1, 16, 6));

            assertThat(callFan(team).getOneLiner().getText()).isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesACachedSentenceThatCarriesNoTimestamp() throws Exception {
            TeamData team = TestFixtures.teamDataWithStanding(
                    TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5);
            team.addOneLiner(TeamOneLiner.builder()
                    .language(Language.BRITISH).competition(Competition.PREMIER_LEAGUE)
                    .perspective(Perspective.FAN).text("no timestamp").build());

            assertThat(callFan(team).getOneLiner().getText()).isEqualTo("a freshly generated sentence");
        }

        /**
         * The trap in §5: a Set whose equality ignores the text swallows an add() of an entry
         * that already exists, so the stale sentence would survive a regeneration.
         */
        @Test
        void aRegenerationOverwritesTheStoredSentenceRatherThanBeingSwallowed() throws Exception {
            TeamData team = teamWithCachedOneLiner("the stale sentence");
            team.getStandings().put(Competition.PREMIER_LEAGUE,
                    TestFixtures.standing(Competition.PREMIER_LEAGUE, 4, 13, 5));

            callFan(team);

            assertThat(team.getOneLiners()).hasSize(1);
            assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN))
                    .get()
                    .extracting(TeamOneLiner::getText)
                    .isEqualTo("a freshly generated sentence");
            verify(teamDataService).save(team);
        }

        @Test
        void storesTheStandingSnapshotItWasWrittenAgainst() throws Exception {
            TeamData team = TestFixtures.teamDataWithStanding(
                    TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 3, 9, 5);

            TeamOneLiner generated = callFan(team).getOneLiner();

            assertThat(generated.getPositionAtGeneration()).isEqualTo(3);
            assertThat(generated.getPointsAtGeneration()).isEqualTo(9);
            assertThat(generated.getGeneratedAt()).isNotNull();
        }

        @Test
        void holdsAnIndependentEntryPerPerspectiveAndLanguage() throws Exception {
            TeamData team = teamWithCachedOneLiner("the fan sentence");
            when(teamDataService.getTeamById(TEAM_ID)).thenReturn(team);

            service.getTeamSmallTalk(TEAM_ID, null, Language.BRITISH, Perspective.RIVAL_FAN);
            service.getTeamSmallTalk(TEAM_ID, null, Language.HEBREW, Perspective.FAN);

            assertThat(team.getOneLiners()).hasSize(3);
            assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN))
                    .get()
                    .extracting(TeamOneLiner::getText)
                    .isEqualTo("the fan sentence");
        }
    }

    @Nested
    class CompetitionResolution {

        private TeamData multiCompetitionTeam() {
            TeamData team = TestFixtures.teamDataWithStanding(
                    TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5);
            team.getStandings().put(Competition.CHAMPIONS_LEAGUE,
                    TestFixtures.standing(Competition.CHAMPIONS_LEAGUE, 2, 4, 2));
            return team;
        }

        @Test
        void prefersTheDomesticLeagueOverTheChampionsLeague() throws Exception {
            TeamSmallTalk result = callFan(multiCompetitionTeam());

            assertThat(result.getFacts().getPrimaryCompetition()).isEqualTo(Competition.PREMIER_LEAGUE);
        }

        @Test
        void honoursAnExplicitlyRequestedCompetition() throws Exception {
            when(teamDataService.getTeamById(TEAM_ID)).thenReturn(multiCompetitionTeam());

            TeamSmallTalk result = service.getTeamSmallTalk(
                    TEAM_ID, Competition.CHAMPIONS_LEAGUE, Language.BRITISH, Perspective.FAN);

            assertThat(result.getFacts().getPrimaryCompetition()).isEqualTo(Competition.CHAMPIONS_LEAGUE);
        }

        @Test
        void ignoresARequestedCompetitionTheTeamDoesNotPlayIn() throws Exception {
            when(teamDataService.getTeamById(TEAM_ID)).thenReturn(multiCompetitionTeam());

            TeamSmallTalk result = service.getTeamSmallTalk(
                    TEAM_ID, Competition.LA_LIGA, Language.BRITISH, Perspective.FAN);

            assertThat(result.getFacts().getPrimaryCompetition()).isEqualTo(Competition.PREMIER_LEAGUE);
        }

        @Test
        void breaksATieBetweenDomesticLeaguesOnMatchesPlayed() throws Exception {
            TeamData team = TestFixtures.teamDataWithStanding(
                    TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.LIGAT_HA_AL, 4, 8, 4);
            team.getStandings().put(Competition.PREMIER_LEAGUE,
                    TestFixtures.standing(Competition.PREMIER_LEAGUE, 1, 30, 12));

            assertThat(callFan(team).getFacts().getPrimaryCompetition())
                    .isEqualTo(Competition.PREMIER_LEAGUE);
        }

        /** League position is meaningless for a national side, so we say nothing at all (§6.5). */
        @Test
        void rejectsATeamWhoseOnlyStandingIsTheWorldCup() throws Exception {
            TeamData england = TestFixtures.teamDataWithStanding(
                    "england", "England", "Thomas Tuchel", Competition.WORLD_CUP, 1, 9, 3);
            when(teamDataService.getTeamById("england")).thenReturn(england);

            assertThatThrownBy(() -> service.getTeamSmallTalk(
                    "england", null, Language.BRITISH, Perspective.NEUTRAL))
                    .isInstanceOf(SmallTalkException.class)
                    .hasMessage(Messages.TEAM_HAS_NO_LEAGUE_STANDING);
        }

        @Test
        void rejectsATeamWithNoStandingsAtAll() throws Exception {
            when(teamDataService.getTeamById(TEAM_ID))
                    .thenReturn(TestFixtures.teamData(TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot"));

            assertThatThrownBy(() -> service.getTeamSmallTalk(
                    TEAM_ID, null, Language.BRITISH, Perspective.NEUTRAL))
                    .isInstanceOf(SmallTalkException.class)
                    .hasMessage(Messages.TEAM_HAS_NO_LEAGUE_STANDING);
        }

        /** A club side that also has a World Cup entry is unaffected. */
        @Test
        void aClubWithAWorldCupEntryStillGetsItsLeague() throws Exception {
            TeamData team = TestFixtures.teamDataWithStanding(
                    TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5);
            team.getStandings().put(Competition.WORLD_CUP,
                    TestFixtures.standing(Competition.WORLD_CUP, 1, 9, 3));

            assertThat(callFan(team).getFacts().getPrimaryCompetition())
                    .isEqualTo(Competition.PREMIER_LEAGUE);
        }
    }

    @Nested
    class Facts {

        @Test
        void carriesTheFormNextFixtureAndNotablePlayersFromTheirServices() throws Exception {
            Fixture next = TestFixtures.upcomingFixture().build();
            when(fixtureService.getRecentFinishedForTeam(TEAM_ID, 5))
                    .thenReturn(List.of(playedAt(GENERATED_AT.minus(2, ChronoUnit.DAYS))));
            when(fixtureService.getNextFixtureForTeam(TEAM_ID)).thenReturn(Optional.of(next));

            TeamSmallTalk result = callFan(teamWithCachedOneLiner("cached"));

            assertThat(result.getFacts().getRecentForm()).hasSize(1);
            assertThat(result.getFacts().getRecentForm().get(0).getOpponent())
                    .isEqualTo(TestFixtures.AWAY_TEAM_NAME);
            assertThat(result.getFacts().getRecentForm().get(0).getResult())
                    .isEqualTo(TeamFacts.Result.WIN);
            assertThat(result.getFacts().getNextFixture().getOpponent())
                    .isEqualTo(TestFixtures.AWAY_TEAM_NAME);
            assertThat(result.getFacts().getNextFixture().isHome()).isTrue();
        }

        @Test
        void propagatesAnUnknownTeamAsANotFound() throws Exception {
            when(teamDataService.getTeamById("nope"))
                    .thenThrow(new NotFoundException(Messages.NO_TEAM_FOUND.formatted("nope")));

            assertThatThrownBy(() -> service.getTeamSmallTalk("nope", null, Language.BRITISH, Perspective.FAN))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
