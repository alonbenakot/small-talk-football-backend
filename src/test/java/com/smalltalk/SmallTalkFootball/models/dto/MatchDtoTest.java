package com.smalltalk.SmallTalkFootball.models.dto;

import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MatchDto is deserialized by the {@code @Qualifier("apiClient")} ObjectMapper, which uses
 * SNAKE_CASE. Three properties - the two team names and the lineup subtree - have getters
 * spelled differently from their fields, so Jackson never bound them until each field was
 * given an explicit {@code @JsonProperty} with the verbatim wire name (bugs.md #3).
 * <p>
 * Jackson does not run a naming strategy over an explicitly named property, so this exercises
 * the SNAKE_CASE-plus-explicit-@JsonProperty interaction rather than assuming it.
 */
class MatchDtoTest {

    private static final String PAYLOAD = """
            {
              "match_id": "9001",
              "match_hometeam_name": "Liverpool",
              "match_awayteam_name": "Everton",
              "match_hometeam_score": "3",
              "match_awayteam_score": "1",
              "lineup": {
                "home": { "coach": [ { "lineup_player": "Arne Slot" } ] },
                "away": { "coach": [ { "lineup_player": "David Moyes" } ] }
              }
            }
            """;

    @Test
    @DisplayName("the differently-named team-name getters bind from the snake_case payload")
    void bindsTeamNames() {
        MatchDto dto = JsonFixtures.parse(PAYLOAD, MatchDto.class);

        assertThat(dto.getMatchHomeTeamName()).isEqualTo("Liverpool");
        assertThat(dto.getMatchAwayTeamName()).isEqualTo("Everton");
    }

    @Test
    @DisplayName("the whole lineup subtree binds at every level")
    void bindsTheLineupSubtree() {
        MatchDto dto = JsonFixtures.parse(PAYLOAD, MatchDto.class);

        assertThat(dto.getMatchLineup()).isNotNull();
        assertThat(dto.getMatchLineup().getHomeLineUp().getCoaches().get(0).getLineupPlayer())
                .isEqualTo("Arne Slot");
        assertThat(dto.getMatchLineup().getAwayLineUp().getCoaches().get(0).getLineupPlayer())
                .isEqualTo("David Moyes");
    }

    @Test
    @DisplayName("fields whose getter already matched the field name still bind")
    void doesNotRegressTheAlreadyWorkingFields() {
        MatchDto dto = JsonFixtures.parse(PAYLOAD, MatchDto.class);

        assertThat(dto.getMatchId()).isEqualTo("9001");
        assertThat(dto.getMatchHometeamScore()).isEqualTo("3");
    }
}
