package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;

import java.util.List;

/**
 * The dry facts behind a player one-liner, shaped for the card (plan §2.1). A response type
 * rather than the {@link PlayerData} document itself, so the cached one-liner set cannot leak
 * into the body. {@code season} echoes the stored stats flat; its nulls mean "not recorded"
 * and are deliberately not coerced to zero. {@code team}, {@code competition} and
 * {@code teamStanding} are null when the club no longer resolves.
 */
public record PlayerFacts(String id,
                          String name,
                          String image,
                          String number,
                          String position,
                          String age,
                          boolean captain,
                          boolean injured,
                          Club team,
                          Competition competition,
                          Season season,
                          Integer leagueScorerRank,
                          SquadContext squadContext,
                          Standing teamStanding,
                          List<MatchContribution> recentContributions,
                          TeamFacts.NextFixture nextFixture) {

    public record Club(String id, String name, String crest, String coach) {
    }

    public record Season(Integer matchesPlayed, Integer goals, Integer assists, Integer shotsTotal,
                         Integer keyPasses, Integer passes, Integer passesAccurate, Integer tackles,
                         Integer interceptions, Integer clearances, Integer duelsTotal, Integer duelsWon,
                         Integer yellowCards, Integer redCards, String rating,
                         Integer saves, Integer insideBoxSaves, Integer goalsConceded) {
    }

    public static PlayerFacts from(PlayerData player, SquadContext squadContext, TeamData team,
                                   Competition competition, List<MatchContribution> recentContributions,
                                   Fixture nextFixture) {
        Standing standing = team == null || competition == null || team.getStandings() == null
                ? null : team.getStandings().get(competition);

        return new PlayerFacts(
                player.getId(), player.getName(), player.getImage(), player.getNumber(), player.getPosition(),
                player.getAge(), player.isCaptain(), player.isInjured(),
                team == null ? null : new Club(team.getId(), team.getName(), team.getCrest(), team.getCoach()),
                competition,
                season(player),
                player.getLeagueScorerRank(),
                squadContext,
                standing,
                recentContributions,
                TeamFacts.nextFixture(nextFixture, player.getTeamId()));
    }

    private static Season season(PlayerData p) {
        return new Season(p.getMatchesPlayed(), p.getGoals(), p.getAssists(), p.getShotsTotal(),
                p.getKeyPasses(), p.getPasses(), p.getPassesAccurate(), p.getTackles(),
                p.getInterceptions(), p.getClearances(), p.getDuelsTotal(), p.getDuelsWon(),
                p.getYellowCards(), p.getRedCards(), p.getRating(),
                p.getSaves(), p.getInsideBoxSaves(), p.getGoalsConceded());
    }
}
