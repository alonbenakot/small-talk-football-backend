package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.MatchContribution;
import com.smalltalk.SmallTalkFootball.models.PlayerOneLiner;
import com.smalltalk.SmallTalkFootball.models.PlayerSmallTalk;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PlayerPromptContext;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilder;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PromptBuilderFactory;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * The player one-liner has no TTL: a cached sentence survives until one of the four snapshot
 * fields moves on the stored document (plan §5). Each field is pinned independently, along
 * with the replaceOneLiner trap — PlayerOneLiner equality ignores its text, so a regeneration
 * written with add() would be a silent no-op and the stale sentence would live forever.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerOneLinersServiceTest {

    private static final String PLAYER_ID = "p-1";
    private static final String TEAM_ID = TestFixtures.HOME_TEAM_ID;
    private static final Instant GENERATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Mock
    private PlayerDataService playerDataService;

    @Mock
    private TeamDataService teamDataService;

    @Mock
    private FixtureService fixtureService;

    @Mock
    private AiService aiService;

    @Mock
    private PromptBuilderFactory promptBuilderFactory;

    @Mock
    private PromptBuilder promptBuilder;

    private PlayerOneLinersService service;

    @BeforeEach
    void setUp() {
        service = new PlayerOneLinersService(
                playerDataService, teamDataService, fixtureService, aiService, promptBuilderFactory);

        when(promptBuilderFactory.create(any(PlayerPromptContext.class), any())).thenReturn(promptBuilder);
        when(promptBuilder.buildPrompt()).thenReturn("the prompt");
        when(aiService.generate(any())).thenReturn("a freshly generated sentence");
        when(fixtureService.getRecentFinishedForTeam(any(), anyInt())).thenReturn(List.of());
        when(fixtureService.getNextFixtureForTeam(any())).thenReturn(Optional.empty());
        when(teamDataService.findTeamById(TEAM_ID)).thenReturn(Optional.of(TestFixtures.teamDataWithStanding(
                TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5)));
    }

    /** A striker on 10 apps, 8 goals, 3 assists, fit, first in the charts. */
    private static PlayerData.PlayerDataBuilder salah() {
        return PlayerData.builder()
                .id(PLAYER_ID).teamId(TEAM_ID).name("Mohamed Salah").position("Forwards")
                .matchesPlayed(10).goals(8).assists(3).injured(false).leagueScorerRank(1);
    }

    /** The snapshot a sentence written against {@link #salah()} would carry. */
    private static PlayerOneLiner cached(String text) {
        return PlayerOneLiner.builder()
                .language(Language.BRITISH)
                .text(text)
                .generatedAt(GENERATED_AT)
                .matchesPlayedAtGeneration(10)
                .goalsAtGeneration(8)
                .assistsAtGeneration(3)
                .injuredAtGeneration(false)
                .scorerRankAtGeneration(1)
                .build();
    }

    private static PlayerData withCached(PlayerData.PlayerDataBuilder builder, String text) {
        PlayerData player = builder.build();
        player.addOneLiner(cached(text));
        return player;
    }

    private PlayerSmallTalk call(PlayerData player) throws NotFoundException {
        when(playerDataService.getPlayerById(PLAYER_ID)).thenReturn(player);
        when(playerDataService.getPlayersByTeam(TEAM_ID)).thenReturn(List.of(player));
        return service.getPlayerSmallTalk(PLAYER_ID, Language.BRITISH);
    }

    @Nested
    class Caching {

        @Test
        void reusesTheCachedSentenceWhenNothingHasMoved() throws Exception {
            PlayerSmallTalk result = call(withCached(salah(), "the cached sentence"));

            assertThat(result.oneLiner().getText()).isEqualTo("the cached sentence");
            verify(aiService, never()).generate(any());
            verify(playerDataService, never()).save(any());
        }

        @Test
        void regeneratesAfterAnAppearance() throws Exception {
            assertThat(call(withCached(salah().matchesPlayed(11), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesAfterAGoal() throws Exception {
            assertThat(call(withCached(salah().goals(9), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesAfterAnAssist() throws Exception {
            assertThat(call(withCached(salah().assists(4), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesAfterTheInjuryFlagFlips() throws Exception {
            assertThat(call(withCached(salah().injured(true), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesAfterAScorerRankMove() throws Exception {
            assertThat(call(withCached(salah().leagueScorerRank(2), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
            assertThat(call(withCached(salah().leagueScorerRank(null), "stale")).oneLiner().getText())
                    .isEqualTo("a freshly generated sentence");
        }

        @Test
        void regeneratesACachedSentenceThatCarriesNoTimestamp() throws Exception {
            PlayerData player = salah().build();
            player.addOneLiner(PlayerOneLiner.builder().language(Language.BRITISH).text("no timestamp")
                    .matchesPlayedAtGeneration(10).goalsAtGeneration(8).assistsAtGeneration(3)
                    .injuredAtGeneration(false).scorerRankAtGeneration(1).build());

            assertThat(call(player).oneLiner().getText()).isEqualTo("a freshly generated sentence");
        }

        /**
         * The trap in §5: a Set whose equality ignores the text swallows an add() of an entry
         * that already exists, so the stale sentence would survive a regeneration.
         */
        @Test
        void aRegenerationOverwritesTheStoredSentenceRatherThanBeingSwallowed() throws Exception {
            PlayerData player = withCached(salah().goals(9), "the stale sentence");

            call(player);

            assertThat(player.getOneLiners()).hasSize(1);
            assertThat(player.findOneLiner(Language.BRITISH)).get()
                    .extracting(PlayerOneLiner::getText).isEqualTo("a freshly generated sentence");
            verify(playerDataService).save(player);
        }

        @Test
        void storesTheSnapshotItWasWrittenAgainst() throws Exception {
            PlayerOneLiner generated = call(salah().goals(9).injured(true).build()).oneLiner();

            assertThat(generated.getMatchesPlayedAtGeneration()).isEqualTo(10);
            assertThat(generated.getGoalsAtGeneration()).isEqualTo(9);
            assertThat(generated.getAssistsAtGeneration()).isEqualTo(3);
            assertThat(generated.getInjuredAtGeneration()).isTrue();
            assertThat(generated.getScorerRankAtGeneration()).isEqualTo(1);
            assertThat(generated.getGeneratedAt()).isNotNull();
        }

        @Test
        void holdsAnIndependentEntryPerLanguage() throws Exception {
            PlayerData player = withCached(salah(), "the British sentence");
            when(playerDataService.getPlayerById(PLAYER_ID)).thenReturn(player);
            when(playerDataService.getPlayersByTeam(TEAM_ID)).thenReturn(List.of(player));

            service.getPlayerSmallTalk(PLAYER_ID, Language.HEBREW);

            assertThat(player.getOneLiners()).hasSize(2);
            assertThat(player.findOneLiner(Language.BRITISH)).get()
                    .extracting(PlayerOneLiner::getText).isEqualTo("the British sentence");
        }

        /** Live output came back split across two lines with trailing spaces; normalise in code. */
        @Test
        void collapsesAMultiLineAnswerOntoOneLine() throws Exception {
            when(aiService.generate(any())).thenReturn("  Eight in ten.  \n\nTop of the charts.  ");

            assertThat(call(salah().build()).oneLiner().getText()).isEqualTo("Eight in ten. Top of the charts.");
        }
    }

    @Nested
    class Facts {

        @Test
        void carriesTheClubItsStandingAndTheSquadContext() throws Exception {
            PlayerSmallTalk result = call(salah().build());

            assertThat(result.facts().team().name()).isEqualTo(TestFixtures.HOME_TEAM_NAME);
            assertThat(result.facts().competition()).isEqualTo(Competition.PREMIER_LEAGUE);
            assertThat(result.facts().teamStanding().getPosition()).isEqualTo(1);
            assertThat(result.facts().season().goals()).isEqualTo(8);
            assertThat(result.facts().squadContext().leadingScorer()).isTrue();
        }

        @Test
        void handsTheClubAndItsFormToThePromptBuilder() throws Exception {
            call(salah().goals(9).build());

            ArgumentCaptor<PlayerPromptContext> context = ArgumentCaptor.forClass(PlayerPromptContext.class);
            verify(promptBuilderFactory).create(context.capture(), any());
            assertThat(context.getValue().team().getName()).isEqualTo(TestFixtures.HOME_TEAM_NAME);
            assertThat(context.getValue().competition()).isEqualTo(Competition.PREMIER_LEAGUE);
            verify(fixtureService).getRecentFinishedForTeam(TEAM_ID, 5);
        }

        /**
         * A player whose club no longer resolves — bug #11's departed player, or a club dropped
         * from the tracked leagues — still gets a sentence about himself (plan §8).
         */
        @Test
        void aPlayerWhoseTeamNoLongerResolvesStillGetsASentence() throws Exception {
            when(teamDataService.findTeamById(TEAM_ID)).thenReturn(Optional.empty());

            PlayerSmallTalk result = call(salah().build());

            assertThat(result.oneLiner().getText()).isEqualTo("a freshly generated sentence");
            assertThat(result.facts().team()).isNull();
            assertThat(result.facts().competition()).isNull();
            assertThat(result.facts().teamStanding()).isNull();
        }

        @Test
        void aClubWithNoLeagueStandingLeavesTheCompetitionEmptyRatherThanThrowing() throws Exception {
            TeamData england = TestFixtures.teamDataWithStanding(
                    TEAM_ID, "England", "Thomas Tuchel", Competition.WORLD_CUP, 1, 9, 3);
            when(teamDataService.findTeamById(TEAM_ID)).thenReturn(Optional.of(england));

            PlayerSmallTalk result = call(salah().build());

            assertThat(result.facts().team().name()).isEqualTo("England");
            assertThat(result.facts().competition()).isNull();
        }

        @Test
        void propagatesAnUnknownPlayerAsANotFound() throws Exception {
            when(playerDataService.getPlayerById("nope"))
                    .thenThrow(new NotFoundException(Messages.NO_PLAYER_FOUND.formatted("nope")));

            assertThatThrownBy(() -> service.getPlayerSmallTalk("nope", Language.BRITISH))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    /**
     * Phase 3 (plan §4.4): a player's recent contributions are a filter over his club's last
     * five finished fixtures, joined on the scorer / assist ids the feed puts on each goal.
     */
    @Nested
    class RecentContributions {

        private Fixture fixture(String id, Instant date, Goal... goals) {
            return TestFixtures.finishedFixture().id(id).matchDateTime(date)
                    .goals(new ArrayList<>(List.of(goals))).build();
        }

        @Test
        void countsGoalsAndAssistsPerFixtureForThisPlayerOnly() throws Exception {
            Instant latest = Instant.parse("2026-03-08T15:00:00Z");
            Instant earlier = Instant.parse("2026-03-01T20:45:00Z");
            when(fixtureService.getRecentFinishedForTeam(TEAM_ID, 5)).thenReturn(List.of(
                    fixture("fx-2", latest,
                            TestFixtures.goalById(PLAYER_ID, "p-7", TeamType.HOME),
                            TestFixtures.goalById(PLAYER_ID, null, TeamType.HOME),
                            TestFixtures.goalById("p-7", PLAYER_ID, TeamType.HOME),
                            TestFixtures.goalById("away-9", null, TeamType.AWAY)),
                    fixture("fx-1", earlier,
                            TestFixtures.goalById("p-7", "p-8", TeamType.HOME))));

            List<MatchContribution> contributions = call(salah().build()).facts().recentContributions();

            assertThat(contributions).containsExactly(
                    new MatchContribution("fx-2", latest, TestFixtures.AWAY_TEAM_NAME, 2, 1));
        }

        @Test
        void namesTheOpponentFromThePlayersSideOfTheFixture() throws Exception {
            Fixture away = TestFixtures.finishedFixture()
                    .homeTeam(TestFixtures.awayTeam()).awayTeam(TestFixtures.homeTeam())
                    .goals(new ArrayList<>(List.of(TestFixtures.goalById(PLAYER_ID, null, TeamType.AWAY))))
                    .build();
            when(fixtureService.getRecentFinishedForTeam(TEAM_ID, 5)).thenReturn(List.of(away));

            assertThat(call(salah().build()).facts().recentContributions())
                    .singleElement().extracting(MatchContribution::opponent).isEqualTo(TestFixtures.AWAY_TEAM_NAME);
        }

        /** Fixtures ingested before the ids were bound carry none, so they contribute nothing. */
        @Test
        void fixturesWhoseGoalsCarryNoIdsContributeNothing() throws Exception {
            when(fixtureService.getRecentFinishedForTeam(TEAM_ID, 5))
                    .thenReturn(List.of(TestFixtures.finishedFixture().build()));

            assertThat(call(salah().build()).facts().recentContributions()).isEmpty();
        }

        @Test
        void handsTheContributionsToThePromptBuilder() throws Exception {
            when(fixtureService.getRecentFinishedForTeam(TEAM_ID, 5)).thenReturn(List.of(
                    fixture("fx-1", GENERATED_AT, TestFixtures.goalById(PLAYER_ID, null, TeamType.HOME))));

            call(salah().build());

            ArgumentCaptor<PlayerPromptContext> context = ArgumentCaptor.forClass(PlayerPromptContext.class);
            verify(promptBuilderFactory).create(context.capture(), any());
            assertThat(context.getValue().recentContributions())
                    .singleElement().extracting(MatchContribution::goals).isEqualTo(1);
        }
    }
}
