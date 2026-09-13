package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The data block has to carry the player, his standing in the squad and his club's situation,
 * and it has to survive every kind of absence — no club, no standing, no keeper stats, no
 * appearances — as readable lines rather than NPEs or invented zeros.
 */
class PlayerOneLinerPromptBuilderTest {

    private static final String TEAM_ID = TestFixtures.HOME_TEAM_ID;

    private static PlayerData.PlayerDataBuilder salah() {
        return PlayerData.builder()
                .id("p-1").teamId(TEAM_ID).teamName("Liverpool").name("Mohamed Salah").position("Forwards")
                .number("11").age("34").matchesPlayed(10).goals(8).assists(3).rating("7.30").leagueScorerRank(1);
    }

    private static PlayerData benchWarmer() {
        return PlayerData.builder().id("p-2").teamId(TEAM_ID).name("Reserve").position("Defenders").build();
    }

    private static TeamData team() {
        return TestFixtures.teamDataWithStanding(
                TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 2, 13, 5);
    }

    private static String prompt(PlayerData player, TeamData team, Competition competition,
                                 List<Fixture> form, Fixture next) {
        SquadContext squad = SquadContext.of(player, List.of(player, benchWarmer()));
        return new PlayerOneLinerPromptBuilder(
                new PlayerPromptContext(player, squad, team, competition, form, next), Language.BRITISH)
                .buildPrompt();
    }

    private static String fullPrompt(PlayerData player) {
        return prompt(player, team(), Competition.PREMIER_LEAGUE,
                List.of(TestFixtures.finishedFixture().build()), TestFixtures.upcomingFixture().build());
    }

    @Nested
    class DataBlock {

        @Test
        void carriesThePlayerHisClubAndTheClubsStanding() {
            String prompt = fullPrompt(salah().build());

            assertThat(prompt)
                    .contains("making conversation about Mohamed Salah of Liverpool")
                    .contains("Mohamed Salah, Forwards, Liverpool, shirt 11, age 34")
                    .contains("10 apps, 8 goals, 3 assists, rating 7.30, 1 in the league scoring charts")
                    .contains("the squad's leading scorer")
                    .contains("Liverpool: position 2, 13 pts")
                    .contains("Coach: Arne Slot")
                    .contains("at home to Everton")
                    .contains("Liverpool 2-1 Everton");
        }

        @Test
        void leadsWithTheSelectedAngle() {
            assertThat(fullPrompt(salah().build())).contains("Lead with: his place in the league scoring charts");
            assertThat(fullPrompt(salah().injured(true).build())).contains("Lead with: he is a regular who is currently injured");
        }

        @Test
        void speaksTheRequestedLanguage() {
            PlayerData player = salah().build();
            String hebrew = new PlayerOneLinerPromptBuilder(
                    new PlayerPromptContext(player, SquadContext.of(player, List.of(player)), team(),
                            Competition.PREMIER_LEAGUE, List.of(), null), Language.HEBREW).buildPrompt();

            assertThat(hebrew).contains("Hebrew");
        }
    }

    @Nested
    class Degradation {

        @Test
        void aNullClubDegradesToTalkingAboutThePlayerAlone() {
            String prompt = prompt(salah().build(), null, null, List.of(), null);

            assertThat(prompt)
                    .contains("Club: Liverpool, no further club data available")
                    .doesNotContain("League standing");
        }

        @Test
        void aNullStandingReadsAsUnavailable() {
            TeamData noStanding = TestFixtures.teamData(TEAM_ID, TestFixtures.HOME_TEAM_NAME, "Arne Slot");

            assertThat(prompt(salah().build(), noStanding, null, List.of(), null))
                    .contains("Liverpool: standing data unavailable")
                    .contains("No recent fixtures available")
                    .contains("No upcoming fixture scheduled");
        }

        /** A blank in the feed is "not recorded", not "none" (§4.1) — the prompt must never say zero. */
        @Test
        void absentStatsArePhrasedAsNotRecordedRatherThanZero() {
            String prompt = fullPrompt(benchWarmer());

            assertThat(prompt)
                    .contains("not recorded apps, not recorded goals, not recorded assists")
                    .contains("Lead with: he has not played this season")
                    .doesNotContain("conceded");
        }

        @Test
        void aKeeperWithSavesGetsThemAndAnOutfieldPlayerDoesNot() {
            PlayerData keeper = salah().position("Goalkeepers").goals(0).assists(0).leagueScorerRank(null)
                    .saves(21).goalsConceded(9).build();

            assertThat(fullPrompt(keeper)).contains("21 saves, 9 conceded").contains("first-choice goalkeeper");
            assertThat(fullPrompt(salah().build())).doesNotContain("conceded");
        }

        @Test
        void survivesAPlayerWithNothingButAnId() {
            PlayerData bare = PlayerData.builder().id("p-9").build();

            assertThatCode(() -> new PlayerOneLinerPromptBuilder(
                    new PlayerPromptContext(bare, SquadContext.of(bare, List.of(bare)), null, null, List.of(), null),
                    Language.BRITISH).buildPrompt()).doesNotThrowAnyException();
        }
    }

    @Nested
    class Constraints {

        @Test
        void forbidsReachingForTrainingData() {
            assertThat(fullPrompt(salah().build()))
                    .contains("Use only the data provided")
                    .contains("nationality, age, former clubs, transfer value, honours or career history")
                    .contains("Do not speculate about his future")
                    .contains("say something modest and true");
        }

        @Test
        void keepsTheSharedStructureOfTheOtherBuilders() {
            assertThat(fullPrompt(salah().build())).contains("1-2 sentences, under 20 words each, no line breaks, no emojis.");
        }
    }
}
