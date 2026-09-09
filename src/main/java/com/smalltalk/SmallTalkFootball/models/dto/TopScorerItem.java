package com.smalltalk.SmallTalkFootball.models.dto;

import lombok.Getter;

/**
 * One row of a {@code get_topscorers} response. Used only to backfill
 * {@link com.smalltalk.SmallTalkFootball.domain.PlayerData#getLeagueScorerRank()}: the
 * player's {@code player_place} in the league scoring charts, matched to a stored player by
 * {@code player_key} (which equals the {@code player_id} carried by {@code get_teams}).
 * Every field is a string.
 */
@Getter
public class TopScorerItem {

    private String playerPlace;

    private String playerName;

    private String playerKey;

    private String teamName;

    private String teamKey;

    private String goals;

    private String assists;

    private String penaltyGoals;
}
