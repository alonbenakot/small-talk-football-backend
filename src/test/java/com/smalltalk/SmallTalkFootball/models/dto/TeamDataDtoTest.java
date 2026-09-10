package com.smalltalk.SmallTalkFootball.models.dto;

import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the {@code get_teams} binding through the production {@code apiClient} mapper
 * (via {@link JsonFixtures}), so the SNAKE_CASE strategy plus the explicit verbatim
 * {@code @JsonProperty} names on the new fields are genuinely tested rather than assumed —
 * this DTO's getters drift from its fields (bug #3), which is exactly the trap those
 * annotations guard against. The player object is copied from the plan appendix.
 */
class TeamDataDtoTest {

    @Test
    void bindsTeamLevelFieldsIncludingTheNestedVenue() {
        TeamDataDto team = JsonFixtures.parse("""
                {
                  "team_key": "80",
                  "team_name": "Manchester City",
                  "team_country": "England",
                  "team_founded": "1880",
                  "team_badge": "https://apiv3.apifootball.com/badges/80_manchester-city.jpg",
                  "venue": {
                    "venue_name": "Etihad Stadium",
                    "venue_address": "Rowsley Street",
                    "venue_city": "Manchester",
                    "venue_capacity": "55097",
                    "venue_surface": "grass"
                  },
                  "coaches": [{"coach_name": "Enzo Maresca"}],
                  "players": []
                }
                """, TeamDataDto.class);

        assertThat(team.getTeamKey()).isEqualTo("80");
        assertThat(team.getTeamCountry()).isEqualTo("England");
        assertThat(team.getTeamFounded()).isEqualTo("1880");
        assertThat(team.getVenue()).isNotNull();
        assertThat(team.getVenue().getVenueName()).isEqualTo("Etihad Stadium");
        assertThat(team.getVenue().getVenueCapacity()).isEqualTo("55097");
        assertThat(team.getPlayers()).isEmpty();
    }

    @Test
    void bindsThePlayersArray() {
        TeamDataDto team = JsonFixtures.parse("""
                {
                  "team_key": "80",
                  "team_name": "Manchester City",
                  "players": [{
                    "player_key": 659972248, "player_id": "659972248",
                    "player_name": "Erling Haaland", "player_number": "9", "player_type": "Forwards",
                    "player_age": "26", "player_is_captain": "1",
                    "player_match_played": "10", "player_goals": "8", "player_assists": "0",
                    "player_injured": "No", "player_shots_total": "33",
                    "player_passes": "86", "player_passes_accuracy": "54",
                    "player_duels_won": "17", "player_rating": "7.30"
                  }]
                }
                """, TeamDataDto.class);

        assertThat(team.getPlayers()).hasSize(1);
        PlayerItem haaland = team.getPlayers().get(0);
        assertThat(haaland.getPlayerId()).isEqualTo("659972248");
        assertThat(haaland.getPlayerName()).isEqualTo("Erling Haaland");
        assertThat(haaland.getPlayerType()).isEqualTo("Forwards");
        assertThat(haaland.getPlayerMatchPlayed()).isEqualTo("10");
        assertThat(haaland.getPlayerGoals()).isEqualTo("8");
        assertThat(haaland.getPlayerShotsTotal()).isEqualTo("33");
        assertThat(haaland.getPlayerPassesAccuracy()).isEqualTo("54");
        assertThat(haaland.getPlayerInjured()).isEqualTo("No");
    }
}
