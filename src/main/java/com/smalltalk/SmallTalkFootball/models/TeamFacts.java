package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The dry facts behind a team one-liner, shaped for the card the frontend renders. It is a
 * response type rather than the {@link TeamData} document itself, so the cached one-liner set
 * cannot leak into the body.
 */
@Data
@Builder
public class TeamFacts {

    private String id;

    private String name;

    private String crest;

    private String coach;

    private String founded;

    private Venue venue;

    private Competition primaryCompetition;

    private Map<Competition, Standing> standings;

    private List<FormResult> recentForm;

    private NextFixture nextFixture;

    private List<PlayerData> notablePlayers;

    public enum Result {WIN, DRAW, LOSS}

    @Data
    @Builder
    public static class FormResult {

        private Competition competition;

        private Instant date;

        private String opponent;

        private boolean home;

        private String score;

        private Result result;
    }

    @Data
    @Builder
    public static class NextFixture {

        private String fixtureId;

        private String opponent;

        private boolean home;

        private Instant kickOff;
    }

    public static TeamFacts from(TeamData team,
                                 Competition primaryCompetition,
                                 List<Fixture> recentForm,
                                 Fixture nextFixture,
                                 List<PlayerData> notablePlayers) {
        return TeamFacts.builder()
                .id(team.getId())
                .name(team.getName())
                .crest(team.getCrest())
                .coach(team.getCoach())
                .founded(team.getFounded())
                .venue(team.getVenue())
                .primaryCompetition(primaryCompetition)
                .standings(team.getStandings())
                .recentForm(recentForm.stream().map(fixture -> formResult(fixture, team)).toList())
                .nextFixture(nextFixture(nextFixture, team))
                .notablePlayers(notablePlayers)
                .build();
    }

    private static FormResult formResult(Fixture fixture, TeamData team) {
        boolean home = isHome(fixture, team);
        Score score = fixture.getScore();

        return FormResult.builder()
                .competition(fixture.getCompetition())
                .date(fixture.getMatchDateTime())
                .opponent(home ? nameOf(fixture.getAwayTeam()) : nameOf(fixture.getHomeTeam()))
                .home(home)
                .score(score == null ? null : "%d-%d".formatted(score.getHome(), score.getAway()))
                .result(result(score, home))
                .build();
    }

    private static Result result(Score score, boolean home) {
        if (score == null) {
            return null;
        }
        if (score.isDraw() || score.getHome() == score.getAway()) {
            return Result.DRAW;
        }
        boolean homeWon = score.getHome() > score.getAway();
        return homeWon == home ? Result.WIN : Result.LOSS;
    }

    private static NextFixture nextFixture(Fixture fixture, TeamData team) {
        if (fixture == null) {
            return null;
        }
        boolean home = isHome(fixture, team);
        return NextFixture.builder()
                .fixtureId(fixture.getId())
                .opponent(home ? nameOf(fixture.getAwayTeam()) : nameOf(fixture.getHomeTeam()))
                .home(home)
                .kickOff(fixture.getMatchDateTime())
                .build();
    }

    /**
     * Matched on id rather than name: the id is what the query selected on, and team names in
     * the feed are not guaranteed to match the stored one character for character.
     */
    private static boolean isHome(Fixture fixture, TeamData team) {
        return fixture.getHomeTeam() != null && team.getId() != null
                && team.getId().equals(fixture.getHomeTeam().getId());
    }

    private static String nameOf(Team team) {
        return team == null ? null : team.getName();
    }
}
