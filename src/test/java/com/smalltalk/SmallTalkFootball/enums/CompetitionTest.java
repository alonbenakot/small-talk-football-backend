package com.smalltalk.SmallTalkFootball.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Competition is the whitelist of tracked leagues: every fetch in the application iterates
 * Competition.values(), so its codes decide what data the system sees at all.
 */
class CompetitionTest {

    @ParameterizedTest
    @EnumSource(Competition.class)
    void everyCompetitionResolvesBackFromItsCode(Competition competition) {
        assertThat(Competition.fromCode(competition.getCode())).isEqualTo(competition);
        assertThat(Competition.isValidCode(competition.getCode())).isTrue();
    }

    @Test
    void codesAreUnique() {
        assertThat(Arrays.stream(Competition.values()).map(Competition::getCode).distinct())
                .hasSize(Competition.values().length);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 999, 153})
    void rejectsCodesOutsideTheWhitelist(int code) {
        assertThat(Competition.isValidCode(code)).isFalse();
        assertThatThrownBy(() -> Competition.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(code));
    }

    @Test
    void pinsTheApifootballLeagueIds() {
        assertThat(Competition.WORLD_CUP.getCode()).isEqualTo(28);
        assertThat(Competition.PREMIER_LEAGUE.getCode()).isEqualTo(152);
        assertThat(Competition.LA_LIGA.getCode()).isEqualTo(302);
        assertThat(Competition.BUNDESLIGA.getCode()).isEqualTo(175);
        assertThat(Competition.LIGAT_HA_AL.getCode()).isEqualTo(202);
        assertThat(Competition.SERIA_A.getCode()).isEqualTo(207);
        assertThat(Competition.CHAMPIONS_LEAGUE.getCode()).isEqualTo(3);
    }
}
