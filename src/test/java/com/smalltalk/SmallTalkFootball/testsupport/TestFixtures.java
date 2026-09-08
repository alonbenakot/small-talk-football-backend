package com.smalltalk.SmallTalkFootball.testsupport;

import com.smalltalk.SmallTalkFootball.domain.Article;
import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.domain.User;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Role;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

/**
 * Ready-made domain objects for tests. Every builder returns a fully populated object so a
 * test only has to state the one thing it actually cares about.
 */
public final class TestFixtures {

    public static final String HOME_TEAM_ID = "2621";
    public static final String AWAY_TEAM_ID = "2622";
    public static final String HOME_TEAM_NAME = "Liverpool";
    public static final String AWAY_TEAM_NAME = "Everton";

    private TestFixtures() {
    }

    public static Team homeTeam() {
        return team(HOME_TEAM_ID, HOME_TEAM_NAME, "Arne Slot");
    }

    public static Team awayTeam() {
        return team(AWAY_TEAM_ID, AWAY_TEAM_NAME, "David Moyes");
    }

    public static Team team(String id, String name, String coach) {
        return Team.builder()
                .id(id)
                .name(name)
                .coach(coach)
                .crest(id + "-badge.png")
                .formation("4-3-3")
                .build();
    }

    public static Score score(int home, int away, String winner) {
        return Score.builder()
                .home(home)
                .away(away)
                .draw(home == away)
                .winner(winner)
                .build();
    }

    /**
     * A completed 2-1 home win with one goal per side and one statistic.
     */
    public static Fixture.FixtureBuilder finishedFixture() {
        return baseFixture()
                .finished(true)
                .matchDateTime(Instant.parse("2026-03-01T20:45:00Z"))
                .score(score(2, 1, HOME_TEAM_NAME))
                .goals(new ArrayList<>(List.of(
                        goal(23, "Salah", "Jones", HOME_TEAM_NAME, TeamType.HOME, 1, 0),
                        goal(67, "Calvert-Lewin", null, AWAY_TEAM_NAME, TeamType.AWAY, 1, 1))))
                .statistics(new ArrayList<>(List.of(
                        statistic("Ball Possession", 60, 40, true),
                        statistic("On Target", 8, 3, false))));
    }

    /**
     * A match that has not been played yet.
     */
    public static Fixture.FixtureBuilder upcomingFixture() {
        return baseFixture()
                .finished(false)
                .matchDateTime(Instant.parse("2026-04-01T18:00:00Z"))
                .score(score(0, 0, null))
                .goals(new ArrayList<>())
                .statistics(new ArrayList<>());
    }

    private static Fixture.FixtureBuilder baseFixture() {
        return Fixture.builder()
                .id("fixture-1")
                .externalId(9001)
                .competition(Competition.PREMIER_LEAGUE)
                .venue("Anfield")
                .homeTeam(homeTeam())
                .awayTeam(awayTeam())
                // The Lombok builder ignores the field initializer, so this has to be explicit
                // or the fixture comes out with a null one-liner set.
                .oneLiners(new HashSet<>());
    }

    public static Goal goal(int minute, String goalBy, String assistBy, String teamName,
                            TeamType teamType, int homeScore, int awayScore) {
        return Goal.builder()
                .minute(minute)
                .goalBy(goalBy)
                .assistBy(assistBy)
                .teamName(teamName)
                .teamType(teamType)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();
    }

    public static Statistic statistic(String type, int home, int away, boolean percentage) {
        return Statistic.builder()
                .type(type)
                .home(home)
                .away(away)
                .percentage(percentage)
                .build();
    }

    public static TeamData teamData(String id, String name, String coach) {
        return TeamData.builder()
                .id(id)
                .name(name)
                .coach(coach)
                .crest(id + "-badge.png")
                .standings(new EnumMap<>(Competition.class))
                .build();
    }

    public static List<TeamData> bothTeamsData() {
        return List.of(
                teamData(HOME_TEAM_ID, HOME_TEAM_NAME, "Arne Slot"),
                teamData(AWAY_TEAM_ID, AWAY_TEAM_NAME, "David Moyes"));
    }

    public static User member(String email) {
        return user(email, Role.MEMBER);
    }

    public static User admin(String email) {
        return user(email, Role.ADMIN);
    }

    public static User user(String email, Role role) {
        User user = new User("Ada", "Lovelace", email, "s3cret", false);
        user.setId("user-" + email);
        user.setRole(role);
        user.setUserIndications(new UserIndications(false, Language.BRITISH));
        return user;
    }

    public static Article article(String id, String title, boolean published) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setAuthor("System Article");
        article.setText("Some body text.");
        article.setPublished(published);
        return article;
    }
}
