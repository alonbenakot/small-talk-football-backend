package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Statistic;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.SummaryMatchDto;
import com.smalltalk.SmallTalkFootball.testsupport.MatchDtoJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FixtureAssembler turns loosely-typed strings from apifootball.com into domain objects.
 * It is the densest logic in the codebase and the place most likely to break, so these
 * tests concentrate on the parsing edge cases rather than on field-for-field copying.
 */
class FixtureAssemblerTest {

    private final FixtureAssembler assembler = new FixtureAssembler();

    @Nested
    @DisplayName("basic assembly")
    class BasicAssembly {

        @Test
        void mapsIdentityAndVenueFields() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_id", "9001")
                            .field("match_stadium", "Anfield")
                            .build());

            assertThat(fixture.getExternalId()).isEqualTo(9001);
            assertThat(fixture.getVenue()).isEqualTo("Anfield");
            assertThat(fixture.getCompetition()).isEqualTo(Competition.PREMIER_LEAGUE);
        }

        @Test
        void mapsTeamIdCrestAndFormation() {
            Fixture fixture = assembler.assembleFromFullMatch(MatchDtoJson.finishedMatch().build());

            assertThat(fixture.getHomeTeam().getId()).isEqualTo("2621");
            assertThat(fixture.getHomeTeam().getCrest()).isEqualTo("home-badge.png");
            assertThat(fixture.getHomeTeam().getFormation()).isEqualTo("4-3-3");
            assertThat(fixture.getAwayTeam().getId()).isEqualTo("2622");
            assertThat(fixture.getAwayTeam().getFormation()).isEqualTo("4-4-2");
        }

        @Test
        void combinesDateAndTimeAsUtc() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_date", "2026-03-01")
                            .field("match_time", "20:45")
                            .build());

            assertThat(fixture.getMatchDateTime()).isEqualTo(Instant.parse("2026-03-01T20:45:00Z"));
        }
    }

    @Nested
    @DisplayName("team name and coach are not populated from the match feed")
    class UnboundFields {

        /*
         * These assert a known gap rather than desirable behaviour. In MatchDto the getters
         * getMatchHomeTeamName() and getMatchLineup() are named differently from their backing
         * fields (matchHometeamName, lineup), so Jackson never makes those private fields
         * visible and they stay null no matter what the payload contains.
         *
         * Nothing breaks downstream today because TeamDataService.enrichTeamsData backfills
         * names and coaches from the get_teams endpoint. These tests pin the current behaviour;
         * if the DTO is ever fixed they will fail and should be updated, not deleted.
         */

        @Test
        void teamNamesAreNullEvenWhenThePayloadHasThem() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_hometeam_name", "Liverpool")
                            .field("match_awayteam_name", "Everton")
                            .build());

            assertThat(fixture.getHomeTeam().getName()).isNull();
            assertThat(fixture.getAwayTeam().getName()).isNull();
        }

        @Test
        void coachIsNullEvenWhenTheLineupHasOne() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().withLineup("Arne Slot", "David Moyes").build());

            assertThat(fixture.getHomeTeam().getCoach()).isNull();
            assertThat(fixture.getAwayTeam().getCoach()).isNull();
        }

        @Test
        void winnerIsNullBecauseItIsDerivedFromTheUnboundTeamName() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_hometeam_score", "3")
                            .field("match_awayteam_score", "1")
                            .build());

            assertThat(fixture.getScore().getHome()).isEqualTo(3);
            assertThat(fixture.getScore().getAway()).isEqualTo(1);
            assertThat(fixture.getScore().isDraw()).isFalse();
            assertThat(fixture.getScore().getWinner()).isNull();
        }

        @Test
        void missingLineupIsToleratedRatherThanThrowing() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().withoutLineup().build());

            assertThat(fixture.getHomeTeam().getCoach()).isNull();
        }
    }

    @Nested
    @DisplayName("finished flag")
    class FinishedFlag {

        @ParameterizedTest
        @ValueSource(strings = {"Finished", "After Pen."})
        void treatsBothCompletedStatusesAsFinished(String status) {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_status", status)
                            .homeGoal("30", "Salah", "1 - 0")
                            .build());

            assertThat(fixture.isFinished()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Live", "Half Time", "Postponed", "finished"})
        void treatsEverythingElseAsNotFinished(String status) {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().field("match_status", status).build());

            assertThat(fixture.isFinished()).isFalse();
        }
    }

    @Nested
    @DisplayName("competition")
    class CompetitionMapping {

        @ParameterizedTest
        @CsvSource({"152,PREMIER_LEAGUE", "3,CHAMPIONS_LEAGUE", "28,WORLD_CUP", "302,LA_LIGA"})
        void mapsKnownLeagueIds(String leagueId, Competition expected) {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().field("league_id", leagueId).build());

            assertThat(fixture.getCompetition()).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"999", "not-a-number", ""})
        void swallowsUnknownOrUnparseableLeagueIds(String leagueId) {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().field("league_id", leagueId).build());

            assertThat(fixture.getCompetition()).isNull();
        }
    }

    @Nested
    @DisplayName("score")
    class ScoreMapping {

        @Test
        void readsTheReportedScoreForAnOrdinaryResult() {
            Score score = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_hometeam_score", "2")
                            .field("match_awayteam_score", "2")
                            .build())
                    .getScore();

            assertThat(score.getHome()).isEqualTo(2);
            assertThat(score.getAway()).isEqualTo(2);
            assertThat(score.isDraw()).isTrue();
            assertThat(score.getWinner()).isNull();
        }

        @Test
        void fallsBackToTheLastGoalWhenTheReportedScoreIsUnparseable() {
            Score score = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_hometeam_score", "")
                            .field("match_awayteam_score", "")
                            .homeGoal("12", "Salah", "1 - 0")
                            .awayGoal("55", "Calvert-Lewin", "1 - 1")
                            .homeGoal("77", "Gakpo", "2 - 1")
                            .build())
                    .getScore();

            assertThat(score.getHome()).isEqualTo(2);
            assertThat(score.getAway()).isEqualTo(1);
        }

        /*
         * Guards the "bugfix: no score after penalties" commit. A shootout win is reported by
         * the API as the post-penalties score, which is not the score the match finished on,
         * so the running score of the last goal is used instead.
         */
        @Test
        void ignoresTheReportedScoreAfterPenaltiesAndUsesTheLastGoal() {
            Score score = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_status", "After Pen.")
                            .field("match_hometeam_score", "5")
                            .field("match_awayteam_score", "4")
                            .homeGoal("20", "Salah", "1 - 0")
                            .awayGoal("64", "Calvert-Lewin", "1 - 1")
                            .build())
                    .getScore();

            assertThat(score.getHome()).isEqualTo(1);
            assertThat(score.getAway()).isEqualTo(1);
            assertThat(score.isDraw()).isTrue();
        }

        @Test
        void stripsBracketsWhenRecordingIndividualGoals() {
            List<Goal> goals = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .homeGoal("90", "Salah", "[3 - 2]")
                            .build())
                    .getGoals();

            assertThat(goals.get(0).getHomeScore()).isEqualTo(3);
            assertThat(goals.get(0).getAwayScore()).isEqualTo(2);
        }

        /*
         * Pins a defect rather than intended behaviour. mapSingleGoal strips the square
         * brackets apifootball puts around a running score, but deriveScoreFromGoals - which
         * the "After Pen." and unparseable-score paths both rely on - does not, and neither
         * caller guards against it. A bracketed score therefore throws straight out of
         * assembleFromFullMatch and aborts the whole FixtureService ingestion run.
         *
         * If deriveScoreFromGoals is ever taught to strip brackets, this test should be
         * replaced with the assertion that the score comes out as 3-2.
         */
        @Test
        void throwsWhenDerivingAScoreFromABracketedRunningScore() {
            MatchDto afterPenaltiesWithBrackets = MatchDtoJson.finishedMatch()
                    .field("match_status", "After Pen.")
                    .homeGoal("90", "Salah", "[3 - 2]")
                    .build();

            assertThatThrownBy(() -> assembler.assembleFromFullMatch(afterPenaltiesWithBrackets))
                    .isInstanceOf(NumberFormatException.class)
                    .hasMessageContaining("[3");
        }

        @Test
        void throwsWhenTheReportedScoreIsMissingAndTheRunningScoreIsBracketed() {
            MatchDto unparseableWithBrackets = MatchDtoJson.finishedMatch()
                    .field("match_hometeam_score", "")
                    .field("match_awayteam_score", "")
                    .homeGoal("90", "Salah", "[3 - 2]")
                    .build();

            assertThatThrownBy(() -> assembler.assembleFromFullMatch(unparseableWithBrackets))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        void derivesGoallessDrawWhenThereAreNoGoalsToFallBackOn() {
            Score score = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_status", "After Pen.")
                            .emptyGoals()
                            .build())
                    .getScore();

            assertThat(score.getHome()).isZero();
            assertThat(score.getAway()).isZero();
            assertThat(score.isDraw()).isTrue();
        }

        @Test
        void derivesGoallessDrawWhenTheGoalListIsAbsent() {
            Score score = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .field("match_status", "After Pen.")
                            .nullGoals()
                            .build())
                    .getScore();

            assertThat(score.getHome()).isZero();
            assertThat(score.getAway()).isZero();
        }
    }

    @Nested
    @DisplayName("goals")
    class GoalMapping {

        @Test
        void attributesGoalsToTheScoringSide() {
            List<Goal> goals = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .homeGoal("12", "Salah", "1 - 0")
                            .awayGoal("55", "Calvert-Lewin", "1 - 1")
                            .build())
                    .getGoals();

            assertThat(goals).hasSize(2);
            assertThat(goals.get(0).getTeamType()).isEqualTo(TeamType.HOME);
            assertThat(goals.get(0).getGoalBy()).isEqualTo("Salah");
            assertThat(goals.get(1).getTeamType()).isEqualTo(TeamType.AWAY);
            assertThat(goals.get(1).getGoalBy()).isEqualTo("Calvert-Lewin");
        }

        @Test
        void recordsTheRunningScoreAtEachGoal() {
            List<Goal> goals = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .homeGoal("12", "Salah", "1 - 0")
                            .homeGoal("40", "Gakpo", "2 - 0")
                            .build())
                    .getGoals();

            assertThat(goals.get(1).getHomeScore()).isEqualTo(2);
            assertThat(goals.get(1).getAwayScore()).isZero();
        }

        @ParameterizedTest
        @CsvSource({"45,45", "90,90", "90+3,93", "45+2,47"})
        void addsStoppageTimeOntoTheMinute(String reportedTime, int expectedMinute) {
            List<Goal> goals = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .homeGoal(reportedTime, "Salah", "1 - 0")
                            .build())
                    .getGoals();

            assertThat(goals.get(0).getMinute()).isEqualTo(expectedMinute);
        }

        @Test
        void hasNoGoalsWhenTheGoalListIsAbsent() {
            Fixture fixture = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch().nullGoals().build());

            assertThat(fixture.getGoals()).isEmpty();
        }
    }

    @Nested
    @DisplayName("statistics")
    class StatisticMapping {

        @Test
        void keepsOnlyWhitelistedStatisticTypes() {
            List<Statistic> statistics = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .statistic("Ball Possession", "60%", "40%")
                            .statistic("Corners", "7", "3")
                            .statistic("On Target", "8", "2")
                            .build())
                    .getStatistics();

            assertThat(statistics).extracting(Statistic::getType)
                    .containsExactly("Ball Possession", "On Target");
        }

        @Test
        void stripsPercentSignsAndFlagsPercentageStatistics() {
            List<Statistic> statistics = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .statistic("Ball Possession", "60%", "40%")
                            .statistic("Attacks", "112", "88")
                            .build())
                    .getStatistics();

            assertThat(statistics.get(0).getHome()).isEqualTo(60);
            assertThat(statistics.get(0).getAway()).isEqualTo(40);
            assertThat(statistics.get(0).isPercentage()).isTrue();
            assertThat(statistics.get(1).getHome()).isEqualTo(112);
            assertThat(statistics.get(1).isPercentage()).isFalse();
        }

        @Test
        void stripsSquareBracketsFromValues() {
            List<Statistic> statistics = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .statistic("Attacks", "[112]", "[88]")
                            .build())
                    .getStatistics();

            assertThat(statistics.get(0).getHome()).isEqualTo(112);
            assertThat(statistics.get(0).getAway()).isEqualTo(88);
        }

        @ParameterizedTest
        @CsvSource({"'',5", "abc,5", "5,''", "5,abc"})
        void dropsStatisticsWithAnUnusableValueOnEitherSide(String home, String away) {
            List<Statistic> statistics = assembler.assembleFromFullMatch(
                    MatchDtoJson.finishedMatch()
                            .statistic("Attacks", home, away)
                            .build())
                    .getStatistics();

            assertThat(statistics).isEmpty();
        }

        @Test
        void hasNoStatisticsWhenTheListIsAbsent() {
            Fixture fixture = assembler.assembleFromFullMatch(MatchDtoJson.finishedMatch().build());

            assertThat(fixture.getStatistics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("summary matches (head-to-head)")
    class SummaryAssembly {

        @Test
        void mapsTeamNamesBecauseTheSummaryDtoHasSetters() {
            Fixture fixture = assembler.assembleFromSummary(
                    summary("2 - 1 win", "Finished", "2", "1"));

            assertThat(fixture.getHomeTeam().getName()).isEqualTo("Liverpool");
            assertThat(fixture.getAwayTeam().getName()).isEqualTo("Everton");
            assertThat(fixture.getScore().getWinner()).isEqualTo("Liverpool");
            assertThat(fixture.isFinished()).isTrue();
        }

        @Test
        void hasNoScoreWhenTheSummaryScoreIsUnparseable() {
            Fixture fixture = assembler.assembleFromSummary(summary("upcoming", "", "", ""));

            assertThat(fixture.getScore()).isNull();
            assertThat(fixture.isFinished()).isFalse();
        }

        @Test
        void doesNotTreatAfterPenaltiesAsFinished() {
            Fixture fixture = assembler.assembleFromSummary(summary("shootout", "After Pen.", "1", "1"));

            assertThat(fixture.isFinished()).isFalse();
        }

        private SummaryMatchDto summary(String label, String status, String homeScore, String awayScore) {
            SummaryMatchDto dto = new SummaryMatchDto();
            dto.setMatchId("7001");
            dto.setLeagueId("152");
            dto.setMatchStatus(status);
            dto.setMatchHometeamId("2621");
            dto.setMatchAwayteamId("2622");
            dto.setMatchHometeamName("Liverpool");
            dto.setMatchAwayteamName("Everton");
            dto.setMatchHometeamScore(homeScore);
            dto.setMatchAwayteamScore(awayScore);
            return dto;
        }
    }
}
