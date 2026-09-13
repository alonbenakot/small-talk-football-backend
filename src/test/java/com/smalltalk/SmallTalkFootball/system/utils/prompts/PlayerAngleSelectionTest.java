package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PlayerAngleSelection.Angle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One case per angle in plan §6.3, in priority order. The two at the bottom matter most:
 * a fringe or unused player still yields an angle, because a user who asked about him
 * deserves an honest answer rather than an error or an invented story.
 */
class PlayerAngleSelectionTest {

    private static PlayerData.PlayerDataBuilder player(String name, String position, Integer apps, int goals, int assists) {
        return PlayerData.builder().id(name).teamId("80").name(name).position(position)
                .matchesPlayed(apps).goals(goals).assists(assists);
    }

    /** Ten regulars on ten appearances with a goal each, so the median is 10 and nobody stands out. */
    private static List<PlayerData> regulars() {
        return java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> player("regular-" + i, "Midfielders", 10, 1, 1).build())
                .toList();
    }

    private static Angle angleOf(PlayerData subject, List<PlayerData> others) {
        List<PlayerData> squad = new java.util.ArrayList<>(others);
        squad.add(subject);
        return PlayerAngleSelection.select(subject, SquadContext.of(subject, squad));
    }

    @Test
    void anInjuredRegularBeatsAScoringChartsPlace() {
        PlayerData subject = player("striker", "Forwards", 10, 9, 2).injured(true).leagueScorerRank(1).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.INJURED);
    }

    @Test
    void anInjuredFringePlayerIsNotTheInjuredRegularAngle() {
        PlayerData subject = player("reserve", "Defenders", 2, 0, 0).injured(true).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.FRINGE);
    }

    @Test
    void aTopFiveScoringChartsPlaceBeatsBeingTheLeadingContributor() {
        PlayerData subject = player("striker", "Forwards", 10, 9, 2).leagueScorerRank(3).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.LEAGUE_SCORER);
    }

    @Test
    void aLowScoringChartsPlaceDoesNotCount() {
        PlayerData subject = player("striker", "Forwards", 10, 9, 2).leagueScorerRank(16).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.LEADING_CONTRIBUTOR);
    }

    @Test
    void theSquadsClearLeadingContributorGetsThatAngle() {
        PlayerData subject = player("striker", "Forwards", 10, 6, 3).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.LEADING_CONTRIBUTOR);
    }

    @Test
    void theFirstChoiceKeeperWithSavesGetsTheKeeperAngle() {
        PlayerData subject = player("keeper", "Goalkeepers", 10, 0, 0).saves(21).goalsConceded(9).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.KEEPER);
    }

    @Test
    void anEverPresentDefenderWithNoGoalsStillYieldsAnAngle() {
        PlayerData subject = player("centre-back", "Defenders", 10, 0, 0).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.EVER_PRESENT);
    }

    @Test
    void aRegularWithNothingStandingOutIsStillAnswered() {
        PlayerData subject = player("midfielder", "Midfielders", 7, 0, 0).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.REGULAR);
    }

    @Test
    void aFringePlayerYieldsTheHonestLowMinutesAngle() {
        PlayerData subject = player("reserve", "Midfielders", 3, 0, 0).build();

        assertThat(angleOf(subject, regulars())).isEqualTo(Angle.FRINGE);
    }

    @Test
    void aPlayerWithNoAppearancesYieldsTheUnusedAngleRatherThanNothing() {
        assertThat(angleOf(player("unused", "Forwards", null, 0, 0).build(), regulars())).isEqualTo(Angle.UNUSED);
        assertThat(angleOf(player("unused", "Forwards", 0, 0, 0).build(), regulars())).isEqualTo(Angle.UNUSED);
    }

    /** The one squad member who has played is trivially "ever present"; his appearances are the maximum. */
    @Test
    void anUnusedPlayerInASquadWhereNobodyHasPlayedIsStillUnused() {
        PlayerData subject = player("unused", "Forwards", null, 0, 0).build();
        List<PlayerData> squad = List.of(player("also-unused", "Forwards", null, 0, 0).build());

        assertThat(angleOf(subject, squad)).isEqualTo(Angle.UNUSED);
    }
}
