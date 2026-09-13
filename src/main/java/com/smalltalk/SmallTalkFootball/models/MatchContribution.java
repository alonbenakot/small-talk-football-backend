package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.Fixture;

import java.time.Instant;
import java.util.List;

/**
 * A player's goals and assists in one of his club's recent finished fixtures (plan §4.4).
 * The join is on the player id the feed puts on each goal, never on names; a goal ingested
 * before the ids were bound simply carries none and is not attributed.
 */
public record MatchContribution(String fixtureId, Instant date, String opponent, int goals, int assists) {

    public static MatchContribution of(Fixture fixture, String playerId, String teamId) {
        List<Goal> goals = fixture.getGoals();
        boolean home = fixture.getHomeTeam() != null && teamId.equals(fixture.getHomeTeam().getId());
        Team opponent = home ? fixture.getAwayTeam() : fixture.getHomeTeam();

        return new MatchContribution(
                fixture.getId(),
                fixture.getMatchDateTime(),
                opponent == null ? null : opponent.getName(),
                (int) goals.stream().filter(goal -> playerId.equals(goal.getScorerId())).count(),
                (int) goals.stream().filter(goal -> playerId.equals(goal.getAssistId())).count());
    }
}
