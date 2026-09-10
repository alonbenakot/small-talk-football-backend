package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code players} array in a {@code get_teams} response is all strings, and roughly a
 * third of a listed squad is blank across every stat field. These cases pin the tolerant
 * parsing the mapper has to do so that one odd row never aborts a whole squad refresh.
 */
class PlayerDataMapperTest {

    private final PlayerDataMapper mapper = new PlayerDataMapper();

    private static TeamDataDto team(String playersJson) {
        return JsonFixtures.parse("""
                {
                  "team_key": "80",
                  "team_name": "Manchester City",
                  "players": %s
                }
                """.formatted(playersJson), TeamDataDto.class);
    }

    /** A near-verbatim outfield player from the plan appendix. */
    private static final String HAALAND = """
            {
              "player_key": 659972248, "player_id": "659972248",
              "player_name": "Erling Haaland", "player_number": "9", "player_type": "Forwards",
              "player_age": "26", "player_is_captain": "1",
              "player_match_played": "10", "player_goals": "8", "player_assists": "0",
              "player_yellow_cards": "0", "player_red_cards": "0", "player_injured": "No",
              "player_shots_total": "33", "player_passes": "86", "player_passes_accuracy": "54",
              "player_duels_total": "29", "player_duels_won": "17", "player_rating": "7.30"
            }
            """;

    @Test
    void bindsAndConvertsAFullyPopulatedPlayer() {
        List<PlayerData> squad = mapper.map(team("[" + HAALAND + "]"));

        assertThat(squad).hasSize(1);
        PlayerData haaland = squad.get(0);
        assertThat(haaland.getId()).isEqualTo("659972248");
        assertThat(haaland.getTeamId()).isEqualTo("80");
        assertThat(haaland.getTeamName()).isEqualTo("Manchester City");
        assertThat(haaland.getName()).isEqualTo("Erling Haaland");
        assertThat(haaland.getPosition()).isEqualTo("Forwards");
        assertThat(haaland.isCaptain()).isTrue();
        assertThat(haaland.getMatchesPlayed()).isEqualTo(10);
        assertThat(haaland.getGoals()).isEqualTo(8);
        assertThat(haaland.getShotsTotal()).isEqualTo(33);
        assertThat(haaland.getPassesAccurate()).isEqualTo(54);
        assertThat(haaland.isInjured()).isFalse();
        assertThat(haaland.getRating()).isEqualTo("7.30");
    }

    @Test
    void parsesBlankStatsToNullRatherThanThrowing() {
        List<PlayerData> squad = mapper.map(team("""
                [{
                  "player_id": "3907863339", "player_name": "Geronimo Rulli", "player_type": "Goalkeepers",
                  "player_match_played": "", "player_goals": "", "player_assists": "",
                  "player_yellow_cards": "", "player_is_captain": "", "player_saves": "",
                  "player_passes": "", "player_passes_accuracy": "", "player_rating": ""
                }]
                """));

        PlayerData rulli = squad.get(0);
        assertThat(rulli.getMatchesPlayed()).isNull();
        assertThat(rulli.getGoals()).isNull();
        assertThat(rulli.getSaves()).isNull();
        assertThat(rulli.getPasses()).isNull();
        assertThat(rulli.isCaptain()).isFalse();
        assertThat(rulli.getRating()).isEmpty();
    }

    @Test
    void toleratesANonNumericStatValue() {
        List<PlayerData> squad = mapper.map(team("""
                [{"player_id": "1", "player_name": "Odd Row", "player_goals": "N/A", "player_match_played": "five"}]
                """));

        assertThat(squad.get(0).getGoals()).isNull();
        assertThat(squad.get(0).getMatchesPlayed()).isNull();
    }

    @Test
    void readsPlayerInjuredAsABoolean() {
        List<PlayerData> squad = mapper.map(team("""
                [{"player_id": "1", "player_name": "Crocked", "player_injured": "Yes"},
                 {"player_id": "2", "player_name": "Fit", "player_injured": "No"},
                 {"player_id": "3", "player_name": "Unknown"}]
                """));

        assertThat(squad).extracting(PlayerData::isInjured).containsExactly(true, false, false);
    }

    /**
     * An empty {@code players} array is normal for national teams, mirroring the empty-coaches
     * case already pinned in {@link TeamDataUpdateMapperTest}.
     */
    @Test
    void returnsAnEmptyListForATeamWithNoSquad() {
        assertThat(mapper.map(team("[]"))).isEmpty();
        assertThat(mapper.map(JsonFixtures.parse("""
                {"team_key": "1", "team_name": "England"}
                """, TeamDataDto.class))).isEmpty();
    }
}
