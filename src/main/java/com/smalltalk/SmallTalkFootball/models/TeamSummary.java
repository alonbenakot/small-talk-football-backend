package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;

/**
 * The light shape the Teams page lists ({@code GET /teams?competition=}): identity plus the
 * team's place in the requested competition's table.
 */
public record TeamSummary(String id, String name, String crest, Integer position, Integer points) {

    public static TeamSummary from(TeamData team, Competition competition) {
        Standing standing = team.getStandings().get(competition);
        return new TeamSummary(team.getId(), team.getName(), team.getCrest(),
                standing.getPosition(), standing.getPoints());
    }
}
