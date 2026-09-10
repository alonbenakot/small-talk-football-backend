package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.models.dto.PlayerItem;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Turns the {@code players} array embedded in a {@code get_teams} response into
 * {@link PlayerData} documents, carrying the team key and name down from the enclosing
 * {@link TeamDataDto}.
 * <p>
 * Every value in that API is a string, and roughly a third of a listed squad has {@code ""}
 * in every stat field (unused players). So each numeric parse is tolerant: blank or
 * non-numeric yields {@code null}, never a thrown {@code NumberFormatException} that would
 * abort a whole squad refresh (the shape of bug #8). {@code leagueScorerRank} is left null
 * here and backfilled from {@code get_topscorers} by the caller.
 */
@Component
@Qualifier("playerDataMapper")
public class PlayerDataMapper implements Mapper<TeamDataDto, List<PlayerData>> {

    @Override
    public List<PlayerData> map(@NotNull TeamDataDto team) {
        List<PlayerItem> players = team.getPlayers();
        if (players == null || players.isEmpty()) {
            return List.of();
        }
        return players.stream()
                .map(player -> toPlayerData(player, team))
                .toList();
    }

    private PlayerData toPlayerData(PlayerItem player, TeamDataDto team) {
        return PlayerData.builder()
                .id(player.getPlayerId())
                .teamId(team.getTeamKey())
                .teamName(team.getTeamName())
                .name(player.getPlayerName())
                .image(player.getPlayerImage())
                .number(player.getPlayerNumber())
                .position(player.getPlayerType())
                .age(player.getPlayerAge())
                .captain("1".equals(player.getPlayerIsCaptain()))
                .matchesPlayed(toInteger(player.getPlayerMatchPlayed()))
                .goals(toInteger(player.getPlayerGoals()))
                .assists(toInteger(player.getPlayerAssists()))
                .shotsTotal(toInteger(player.getPlayerShotsTotal()))
                .keyPasses(toInteger(player.getPlayerKeyPasses()))
                .passes(toInteger(player.getPlayerPasses()))
                .passesAccurate(toInteger(player.getPlayerPassesAccuracy()))
                .tackles(toInteger(player.getPlayerTackles()))
                .interceptions(toInteger(player.getPlayerInterceptions()))
                .clearances(toInteger(player.getPlayerClearances()))
                .duelsTotal(toInteger(player.getPlayerDuelsTotal()))
                .duelsWon(toInteger(player.getPlayerDuelsWon()))
                .saves(toInteger(player.getPlayerSaves()))
                .insideBoxSaves(toInteger(player.getPlayerInsideBoxSaves()))
                .goalsConceded(toInteger(player.getPlayerGoalsConceded()))
                .yellowCards(toInteger(player.getPlayerYellowCards()))
                .redCards(toInteger(player.getPlayerRedCards()))
                .injured(isInjured(player.getPlayerInjured()))
                .rating(player.getPlayerRating())
                .build();
    }

    /**
     * Parses a stat value, tolerating the blank strings and the occasional non-numeric
     * value the feed carries. Returns {@code null} for anything that is not a plain integer.
     */
    private static Integer toInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isInjured(String value) {
        return "Yes".equalsIgnoreCase(value == null ? null : value.trim());
    }
}
