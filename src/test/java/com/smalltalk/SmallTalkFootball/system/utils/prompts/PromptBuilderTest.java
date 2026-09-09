package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Nested
    class Template {

        /**
         * The interface's default method is the single place the prompt layout is defined, so
         * this checks the sections are all present and in order.
         */
        @Test
        void assemblesEverySectionInOrder() {
            String prompt = new StubPromptBuilder().buildPrompt();

            assertThat(prompt)
                    .contains("THE-ROLE")
                    .contains("Your task:")
                    .contains("THE-TASK")
                    .contains("Tone and Style:")
                    .contains("THE-STYLE")
                    .contains("Structure:")
                    .contains("THE-STRUCTURE")
                    .contains("Constraints:")
                    .contains("THE-CONSTRAINTS")
                    .contains("Examples:")
                    .contains("THE-EXAMPLES")
                    .contains("Your data:")
                    .contains("THE-DATA");

            assertThat(prompt.indexOf("THE-ROLE")).isLessThan(prompt.indexOf("THE-TASK"));
            assertThat(prompt.indexOf("THE-TASK")).isLessThan(prompt.indexOf("THE-STYLE"));
            assertThat(prompt.indexOf("THE-EXAMPLES")).isLessThan(prompt.indexOf("THE-DATA"));
        }

        @Test
        void fencesTheDataSection() {
            assertThat(new StubPromptBuilder().buildPrompt())
                    .contains("##############################");
        }
    }

    @Nested
    class FinishedFixturePrompt {

        private String promptFor(TeamType teamType, Language language) {
            Fixture fixture = TestFixtures.finishedFixture().build();
            return new FinishedFixtureOneLinerPromptBuilder(fixture, teamType, language).buildPrompt();
        }

        @Test
        void speaksAsAFanOfThePreferredTeam() {
            assertThat(promptFor(TeamType.HOME, Language.BRITISH))
                    .contains("You are a Liverpool fan");
            assertThat(promptFor(TeamType.AWAY, Language.BRITISH))
                    .contains("You are a Everton fan");
        }

        @Test
        void leavesThePreferredTeamBlankWhenNoSideIsChosen() {
            String prompt = promptFor(null, Language.BRITISH);

            assertThat(prompt).contains("You are a  fan");
            assertThat(prompt).doesNotContain("Liverpool fan");
        }

        @ParameterizedTest
        @CsvSource({
                "BRITISH,British English",
                "AMERICAN,American English",
                "HEBREW,Hebrew"})
        void namesTheRequestedLanguage(Language language, String expectedDescription) {
            assertThat(promptFor(TeamType.HOME, language)).contains(expectedDescription);
        }

        @Test
        void includesTheMatchFactsTheModelNeeds() {
            String prompt = promptFor(TeamType.HOME, Language.BRITISH);

            assertThat(prompt)
                    .contains("PREMIER_LEAGUE match between Liverpool (home) and Everton (away)")
                    .contains("Liverpool - 2")
                    .contains("Everton - 1")
                    .contains("Winner - Liverpool")
                    .contains("Liverpool coach: Arne Slot")
                    .contains("Everton coach: David Moyes");
        }

        @Test
        void listsGoalsWithScorerMinuteAndRunningScore() {
            String prompt = promptFor(TeamType.HOME, Language.BRITISH);

            assertThat(prompt)
                    .contains("At the minute: 23")
                    .contains("Goal by Salah for Liverpool")
                    .contains("Assist by Jones")
                    .contains("At the minute: 67")
                    .contains("Goal by Calvert-Lewin for Everton");
        }

        @Test
        void omitsTheAssistLineWhenThereWasNoAssist() {
            String prompt = promptFor(TeamType.HOME, Language.BRITISH);

            assertThat(prompt).containsOnlyOnce("Assist by");
        }

        @Test
        void marksPercentageStatisticsWithASign() {
            String prompt = promptFor(TeamType.HOME, Language.BRITISH);

            assertThat(prompt)
                    .contains("Ball Possession")
                    .contains("Home team - 60%")
                    .contains("On Target")
                    .contains("Home team - 8")
                    .doesNotContain("Home team - 8%");
        }

        @Test
        void reportsADrawInsteadOfAWinner() {
            Fixture draw = TestFixtures.finishedFixture()
                    .score(TestFixtures.score(1, 1, null))
                    .build();

            String prompt = new FinishedFixtureOneLinerPromptBuilder(
                    draw, TeamType.HOME, Language.BRITISH).buildPrompt();

            assertThat(prompt).contains("Draw");
            assertThat(prompt).doesNotContain("Winner -");
        }
    }

    private static final class StubPromptBuilder implements PromptBuilder {
        @Override
        public String examples() {
            return "THE-EXAMPLES";
        }

        @Override
        public String role() {
            return "THE-ROLE";
        }

        @Override
        public String task() {
            return "THE-TASK";
        }

        @Override
        public String style() {
            return "THE-STYLE";
        }

        @Override
        public String structure() {
            return "THE-STRUCTURE";
        }

        @Override
        public String constraints() {
            return "THE-CONSTRAINTS";
        }

        @Override
        public String data() {
            return "THE-DATA";
        }
    }
}
