package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;

/**
 * The light shape the squad picker lists ({@code GET /players/teams/{teamId}}) — not the
 * full {@link PlayerData} document, whose stats the card fetches anyway. {@code injured} and
 * {@code matchesPlayed} are here only so the picker can grey out or sort the players nobody
 * will want to ask about.
 */
public record PlayerSummary(String id,
                            String name,
                            String image,
                            String number,
                            String position,
                            boolean injured,
                            Integer matchesPlayed) {

    public static PlayerSummary from(PlayerData player) {
        return new PlayerSummary(player.getId(), player.getName(), player.getImage(), player.getNumber(),
                player.getPosition(), player.isInjured(), player.getMatchesPlayed());
    }
}
