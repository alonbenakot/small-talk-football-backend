package com.smalltalk.SmallTalkFootball.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One player, in its own collection rather than embedded on {@link TeamData}: the next
 * feature (a player one-liner) looks a player up by id, and the team one-liner joins in
 * the squad via {@code teamId}. Written from the {@code players} array of a
 * {@code get_teams} response inside {@code TeamDataService.saveCompetitionTeams}.
 * <p>
 * Every numeric field is a nullable {@link Integer}: apifootball sends {@code ""} (not
 * {@code "0"}) for a squad member with no appearances, and the mapper parses blank as
 * {@code null} rather than throwing.
 * <p>
 * The {@code @Indexed} on {@code teamId} is correct for production and inert under tests,
 * which set {@code spring.data.mongodb.auto-index-creation=false}.
 */
@Document
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String teamName;

    private String name;

    private String image;

    private String number;

    /** Goalkeepers / Defenders / Midfielders / Forwards. */
    private String position;

    private String age;

    private boolean captain;

    private Integer matchesPlayed;

    private Integer goals;

    private Integer assists;

    private Integer shotsTotal;

    private Integer keyPasses;

    private Integer passes;

    /** A count of completed passes, not a percentage — only meaningful as a ratio to {@code passes}. */
    private Integer passesAccurate;

    private Integer tackles;

    private Integer interceptions;

    private Integer clearances;

    private Integer duelsTotal;

    private Integer duelsWon;

    // Keeper-only.
    private Integer saves;

    private Integer insideBoxSaves;

    private Integer goalsConceded;

    private Integer yellowCards;

    private Integer redCards;

    private boolean injured;

    private String rating;

    /** League scoring-charts place from {@code get_topscorers}; null when unranked. */
    private Integer leagueScorerRank;
}
