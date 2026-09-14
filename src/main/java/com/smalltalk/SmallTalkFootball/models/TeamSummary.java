package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;

/**
 * One row of the Teams page ({@code GET /teams}): identity plus the team's place in one
 * competition's table. A club in two tables is two rows with the same id.
 */
public record TeamSummary(String id, String name, String crest,
                          Competition competition, Integer position, Integer points) {

    public static TeamSummary from(TeamData team, Competition competition) {
        Standing standing = team.getStandings().get(competition);
        return new TeamSummary(team.getId(), team.getName(), team.getCrest(),
                competition, standing.getPosition(), standing.getPoints());
    }
}
