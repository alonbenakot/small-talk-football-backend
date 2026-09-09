package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The perspectives are the point of this builder: the same facts have to come out in three
 * genuinely different voices, and the data block has to survive a team with no standing, no
 * form and nobody worth naming — all of which are ordinary states early in a season.
 */
class TeamOneLinerPromptBuilderTest {

    private static final String TEAM_ID = TestFixtures.HOME_TEAM_ID;
    private static final String TEAM_NAME = TestFixtures.HOME_TEAM_NAME;

    private static TeamData team() {
        return TestFixtures.teamDataWithStanding(
                TEAM_ID, TEAM_NAME, "Arne Slot", Competition.PREMIER_LEAGUE, 1, 13, 5);
    }

    private static PlayerData scorer() {
        return PlayerData.builder()
                .id("p-1").teamId(TEAM_ID).name("Mohamed Salah").position("Forwards")
                .matchesPlayed(5).goals(7).assists(2).leagueScorerRank(3)
                .build();
    }

    private static String promptFor(Perspective perspective) {
        return builder(perspective, team(), List.of(TestFixtures.finishedFixture().build()),
                TestFixtures.upcomingFixture().build(), List.of(scorer()));
    }

    private static String builder(Perspective perspective, TeamData team, List<Fixture> form,
                                  Fixture next, List<PlayerData> notable) {
        return new TeamOneLinerPromptBuilder(
                new TeamPromptContext(team, Competition.PREMIER_LEAGUE, form, next, notable),
                Language.BRITISH, perspective).buildPrompt();
    }

    @Nested
    class Voices {

        @Test
        void theFanSpeaksAsASupporterOfTheTeam() {
            assertThat(promptFor(Perspective.FAN))
                    .contains("You support Liverpool and you're talking about your club")
                    .contains("partisan and optimistic");
        }

        @Test
        void theRivalFanIsWindingUpASupporter() {
            assertThat(promptFor(Perspective.RIVAL_FAN))
                    .contains("You support a rival club and you're winding up a Liverpool fan")
                    .contains("teasing and needling");
        }

        @Test
        void theNeutralHasNoAllegiance() {
            assertThat(promptFor(Perspective.NEUTRAL))
                    .contains("You follow football closely")
                    .contains("no allegiance");
        }

        @Test
        void theThreeRolesStylesAndExamplesAllDiffer() {
            String fan = promptFor(Perspective.FAN);
            String rival = promptFor(Perspective.RIVAL_FAN);
            String neutral = promptFor(Perspective.NEUTRAL);

            assertThat(fan).isNotEqualTo(rival).isNotEqualTo(neutral);
            assertThat(rival).isNotEqualTo(neutral);
        }

        /**
         * The rival voice is the one most likely to invent a jibe about a transfer or a
         * manager rumour, so it carries an extra constraint the other two do not.
         */
        @Test
        void onlyTheRivalIsToldToMockOnlyWhatIsInTheData() {
            assertThat(promptFor(Perspective.RIVAL_FAN)).contains("Mock only what is in the data");
            assertThat(promptFor(Perspective.FAN)).doesNotContain("Mock only what is in the data");
            assertThat(promptFor(Perspective.NEUTRAL)).doesNotContain("Mock only what is in the data");
        }

        @Test
        void everyPerspectiveForbidsReachingForTrainingData() {
            for (Perspective perspective : Perspective.values()) {
                assertThat(promptFor(perspective))
                        .contains("Do not mention anything you know about this club that is not listed below");
            }
        }

        @Test
        void aNullPerspectiveFallsBackToNeutral() {
            assertThat(builder(null, team(), List.of(), null, List.of()))
                    .contains("You follow football closely");
        }

        @Test
        void speaksTheRequestedLanguage() {
            String hebrew = new TeamOneLinerPromptBuilder(
                    new TeamPromptContext(team(), Competition.PREMIER_LEAGUE, List.of(), null, List.of()),
                    Language.HEBREW, Perspective.FAN).buildPrompt();

            assertThat(hebrew).contains("Hebrew");
        }
    }

    @Nested
    class DataBlock {

        @Test
        void carriesThePositionPointsAndRecord() {
            assertThat(promptFor(Perspective.NEUTRAL))
                    .contains("Liverpool: position 1, 13 pts (4W 1D 0L)")
                    .contains("Home: 3W 0D 0L | Away: 1W 1D 0L");
        }

        @Test
        void carriesTheRecentForm() {
            assertThat(promptFor(Perspective.NEUTRAL))
                    .contains("Recent form")
                    .contains("Liverpool 2-1 Everton (Win for Liverpool)");
        }

        @Test
        void carriesTheNextFixtureWithTheVenueSide() {
            assertThat(promptFor(Perspective.NEUTRAL))
                    .contains("at home to Everton (PREMIER_LEAGUE)");
        }

        @Test
        void saysWhenTheTeamIsTravelling() {
            Fixture away = TestFixtures.upcomingFixture()
                    .homeTeam(TestFixtures.awayTeam())
                    .awayTeam(TestFixtures.homeTeam())
                    .build();

            assertThat(builder(Perspective.NEUTRAL, team(), List.of(), away, List.of()))
                    .contains("away at Everton");
        }

        @Test
        void carriesTheCoach() {
            assertThat(promptFor(Perspective.FAN)).contains("Coach: Arne Slot");
        }

        @Test
        void carriesTheNotablePlayerWithTheScorerRank() {
            assertThat(promptFor(Perspective.NEUTRAL))
                    .contains("Mohamed Salah (Forwards)")
                    .contains("7 goals, 2 assists")
                    .contains("3 in the league scoring charts");
        }
    }

    @Nested
    class ThinData {

        @Test
        void aMissingStandingDegradesToAReadableLine() {
            TeamData noStanding = TestFixtures.teamData(TEAM_ID, TEAM_NAME, "Arne Slot");

            String prompt = builder(Perspective.NEUTRAL, noStanding, List.of(), null, List.of());

            assertThat(prompt)
                    .contains("Liverpool: standing data unavailable")
                    .contains("Home/away split unavailable");
        }

        @Test
        void anEmptyFormListDegradesToAReadableLine() {
            assertThat(builder(Perspective.FAN, team(), List.of(), null, List.of()))
                    .contains("No recent fixtures available.");
        }

        @Test
        void aMissingNextFixtureDegradesToAReadableLine() {
            assertThat(builder(Perspective.FAN, team(), List.of(), null, List.of()))
                    .contains("No upcoming fixture scheduled.");
        }

        /**
         * Naming nobody is a legitimate outcome of the selection in §6.4, so the prompt has to
         * tell the model what to talk about instead rather than leaving an empty heading.
         */
        @Test
        void anEmptyNotablePlayerListSendsTheModelBackToTheTable() {
            assertThat(builder(Perspective.FAN, team(), List.of(), null, List.of()))
                    .contains("No player is standing out; talk about the table and the form instead.");
        }

        @Test
        void aNullNotablePlayerListIsToleratedToo() {
            assertThat(builder(Perspective.FAN, team(), List.of(), null, null))
                    .contains("No player is standing out");
        }

        @Test
        void aTeamWithNoNameStillReadsProperly() {
            TeamData nameless = TestFixtures.teamData(TEAM_ID, null, null);

            assertThat(builder(Perspective.FAN, nameless, List.of(), null, List.of()))
                    .contains("You support this team")
                    .contains("Coach: unknown");
        }
    }
}
