package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The standings feed reports every number as a string and leaves them blank for competitions
 * that have not started. The mapper's job is to turn blanks into nulls rather than crash.
 */
class StandingMapperTest {

    private final StandingMapper mapper = new StandingMapper();

    private static StandingsDtoItem standings(Map<String, String> overrides) {
        Map<String, String> fields = new LinkedHashMap<>(Map.of(
                "league_id", "152",
                "team_id", "2621",
                "team_name", "Liverpool",
                "overall_league_position", "1",
                "overall_league_payed", "20"));
        fields.putAll(Map.of(
                "overall_league_W", "14",
                "overall_league_D", "4",
                "overall_league_L", "2",
                "home_league_W", "8",
                "home_league_D", "2",
                "home_league_L", "0",
                "away_league_W", "6",
                "away_league_D", "2",
                "away_league_L", "2"));
        fields.putAll(overrides);

        return JsonFixtures.parse(JsonFixtures.apiClientMapper().valueToTree(fields).toString(),
                StandingsDtoItem.class);
    }

    @Test
    void mapsPositionPlayedAndCompetition() {
        Standing standing = mapper.map(standings(Map.of()));

        assertThat(standing.getCompetition()).isEqualTo(Competition.PREMIER_LEAGUE);
        assertThat(standing.getPosition()).isEqualTo(1);
        assertThat(standing.getPlayedMatches()).isEqualTo(20);
    }

    @Test
    void calculatesPointsAsThreePerWinPlusOnePerDraw() {
        Standing standing = mapper.map(standings(Map.of("overall_league_W", "14", "overall_league_D", "4")));

        assertThat(standing.getPoints()).isEqualTo(46);
    }

    @Test
    void mapsOverallHomeAndAwayRecordsSeparately() {
        Standing standing = mapper.map(standings(Map.of()));

        assertThat(standing.getOverall().getWins()).isEqualTo(14);
        assertThat(standing.getOverall().getDraws()).isEqualTo(4);
        assertThat(standing.getOverall().getLosses()).isEqualTo(2);
        assertThat(standing.getHome().getWins()).isEqualTo(8);
        assertThat(standing.getHome().getLosses()).isZero();
        assertThat(standing.getAway().getWins()).isEqualTo(6);
    }

    @Test
    void leavesPointsNullWhenTheRecordIsBlank() {
        assertThat(mapper.map(standings(Map.of("overall_league_W", ""))).getPoints()).isNull();
        assertThat(mapper.map(standings(Map.of("overall_league_D", ""))).getPoints()).isNull();
    }

    @Test
    void leavesPositionAndPlayedNullWhenBlank() {
        Standing standing = mapper.map(standings(Map.of(
                "overall_league_position", "",
                "overall_league_payed", "")));

        assertThat(standing.getPosition()).isNull();
        assertThat(standing.getPlayedMatches()).isNull();
    }

    @Test
    void nullsIndividualRecordComponentsIndependently() {
        Standing standing = mapper.map(standings(Map.of("home_league_D", "")));

        assertThat(standing.getHome().getWins()).isEqualTo(8);
        assertThat(standing.getHome().getDraws()).isNull();
        assertThat(standing.getHome().getLosses()).isZero();
    }

    /*
     * Unlike FixtureAssembler, this mapper does not guard Competition.fromCode. It is not
     * reachable today because standings are fetched one known competition at a time, but a
     * feed carrying any other league id would abort the whole standings refresh.
     */
    @Test
    void throwsOnALeagueIdOutsideTheWhitelist() {
        StandingsDtoItem unknownLeague = standings(Map.of("league_id", "999"));

        assertThatThrownBy(() -> mapper.map(unknownLeague))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
