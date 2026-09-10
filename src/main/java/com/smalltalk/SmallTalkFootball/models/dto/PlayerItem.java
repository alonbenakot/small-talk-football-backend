package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * One entry in the {@code players} array of a {@code get_teams} response. Bound from the
 * live response documented in the plan appendix, <b>not</b> from the published apifootball
 * docs, which list {@code player_minutes} (absent here) and omit roughly twenty fields
 * that are present.
 * <p>
 * Every value arrives as a string, and unused squad members carry {@code ""} rather than
 * {@code "0"} in every stat field, so {@link com.smalltalk.SmallTalkFootball.system.utils.mappers.PlayerDataMapper}
 * parses each one tolerantly. Only the fields this feature needs are bound; the misspelled
 * upstream keys ({@code player_dispossesed}, {@code player_woordworks}) are intentionally
 * left out.
 */
@Getter
public class PlayerItem {

    @JsonProperty("player_id")
    private String playerId;

    @JsonProperty("player_name")
    private String playerName;

    @JsonProperty("player_image")
    private String playerImage;

    @JsonProperty("player_number")
    private String playerNumber;

    /** Goalkeepers / Defenders / Midfielders / Forwards. */
    @JsonProperty("player_type")
    private String playerType;

    @JsonProperty("player_age")
    private String playerAge;

    /** "1" for the captain, "" otherwise. */
    @JsonProperty("player_is_captain")
    private String playerIsCaptain;

    /** Blank, not "0", for a squad member with no appearances. */
    @JsonProperty("player_match_played")
    private String playerMatchPlayed;

    @JsonProperty("player_goals")
    private String playerGoals;

    @JsonProperty("player_assists")
    private String playerAssists;

    @JsonProperty("player_yellow_cards")
    private String playerYellowCards;

    @JsonProperty("player_red_cards")
    private String playerRedCards;

    /** "Yes" / "No" as a string. */
    @JsonProperty("player_injured")
    private String playerInjured;

    @JsonProperty("player_shots_total")
    private String playerShotsTotal;

    @JsonProperty("player_key_passes")
    private String playerKeyPasses;

    @JsonProperty("player_passes")
    private String playerPasses;

    /** A count of completed passes, <b>not</b> a percentage (454 of 478 for Ruben Dias). */
    @JsonProperty("player_passes_accuracy")
    private String playerPassesAccuracy;

    @JsonProperty("player_tackles")
    private String playerTackles;

    @JsonProperty("player_interceptions")
    private String playerInterceptions;

    @JsonProperty("player_clearances")
    private String playerClearances;

    @JsonProperty("player_duels_total")
    private String playerDuelsTotal;

    @JsonProperty("player_duels_won")
    private String playerDuelsWon;

    // Keeper-only stats.
    @JsonProperty("player_saves")
    private String playerSaves;

    @JsonProperty("player_inside_box_saves")
    private String playerInsideBoxSaves;

    @JsonProperty("player_goals_conceded")
    private String playerGoalsConceded;

    @JsonProperty("player_rating")
    private String playerRating;
}
