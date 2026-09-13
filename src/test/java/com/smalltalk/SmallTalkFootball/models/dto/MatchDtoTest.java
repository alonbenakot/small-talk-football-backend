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
              "goalscorer": [
                { "time": "9", "home_scorer": "", "home_scorer_id": "", "home_assist": "", "home_assist_id": "",
                  "score": "0 - 1", "away_scorer": "M. Tavernier", "away_scorer_id": "3819203541",
                  "away_assist": "", "away_assist_id": "" },
                { "time": "40", "home_scorer": "A. Isak", "home_scorer_id": "1001", "home_assist": "B. Bruno",
                  "home_assist_id": "1002", "score": "1 - 1", "away_scorer": "", "away_scorer_id": "",
                  "away_assist": "", "away_assist_id": "" }
              ],
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

    /** Phase 3 of the player one-liner joins goals to players on these ids (plan §4.4). */
    @Test
    @DisplayName("the four scorer and assist ids on a goal bind")
    void bindsTheGoalscorerIds() {
        MatchDto dto = JsonFixtures.parse(PAYLOAD, MatchDto.class);

        GoalscorerItem away = dto.getGoalscorer().get(0);
        assertThat(away.getAwayScorerId()).isEqualTo("3819203541");
        assertThat(away.getAwayAssistId()).isEmpty();
        assertThat(away.getHomeScorerId()).isEmpty();

        GoalscorerItem home = dto.getGoalscorer().get(1);
        assertThat(home.getHomeScorerId()).isEqualTo("1001");
        assertThat(home.getHomeAssistId()).isEqualTo("1002");
    }

    @Test
    @DisplayName("fields whose getter already matched the field name still bind")
    void doesNotRegressTheAlreadyWorkingFields() {
        MatchDto dto = JsonFixtures.parse(PAYLOAD, MatchDto.class);

        assertThat(dto.getMatchId()).isEqualTo("9001");
        assertThat(dto.getMatchHometeamScore()).isEqualTo("3");
    }
}
