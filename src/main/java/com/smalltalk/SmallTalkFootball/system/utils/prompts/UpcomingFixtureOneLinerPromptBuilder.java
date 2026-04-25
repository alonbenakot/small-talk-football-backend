package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.WinLossDraw;

import java.util.List;
import java.util.stream.Collectors;

public class UpcomingFixtureOneLinerPromptBuilder implements PromptBuilder {

    private final Fixture fixture;
    private final Language language;
    private final HeadToHeadData headToHeadData;
    private final TeamData homeTeamData;
    private final TeamData awayTeamData;
    private String preferredTeam;

    public UpcomingFixtureOneLinerPromptBuilder(Fixture fixture, TeamType preferredTeam, Language language,
                                                HeadToHeadData headToHeadData,
                                                TeamData homeTeamData, TeamData awayTeamData) {
        this.fixture = fixture;
        this.language = language;
        this.headToHeadData = headToHeadData;
        this.homeTeamData = homeTeamData;
        this.awayTeamData = awayTeamData;
        setPreferredTeam(preferredTeam);
    }

    public void setPreferredTeam(TeamType teamType) {
        this.preferredTeam = teamType == null
                ? ""
                : teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
    }

    @Override
    public String role() {
        String persona = preferredTeam.isEmpty() ? "football" : preferredTeam;
        return """
                You are a %s fan chatting with your friends about an upcoming football (soccer) match.
                """.formatted(persona);
    }

    @Override
    public String task() {
        return "Generate a casual fan comment hyping or previewing the upcoming match.";
    }

    @Override
    public String style() {
        String bias = preferredTeam.isEmpty() ? "neutral" : "slightly biased";
        return "%s, %s, casual friendly banter.".formatted(getLanguageDescription(), bias);
    }

    @Override
    public String structure() {
        return "1-2 sentences, under 20 words each, no line breaks, no emojis.";
    }

    @Override
    public String constraints() {
        return "No score predictions, no invented events. Base your comment only on the data provided.";
    }

    @Override
    public String examples() {
        return """
                1. Three wins in a row for us, this is the best time to face them.
                2. Every time these two meet it's a war, can't wait.""";
    }

    @Override
    public String data() {
        String home = fixture.getHomeTeam().getName();
        String away = fixture.getAwayTeam().getName();
        String competition = fixture.getCompetition().toString();

        String coaches = """
                %s coach: %s
                %s coach: %s""".formatted(home, fixture.getHomeTeam().getCoach(), away, fixture.getAwayTeam().getCoach());

        String homeStanding = phraseStanding(home, homeTeamData);
        String awayStanding = phraseStanding(away, awayTeamData);

        String homeForm = phraseRecentForm(headToHeadData.getFirstTeamLastFixtures());
        String awayForm = phraseRecentForm(headToHeadData.getSecondTeamLastFixtures());

        String h2h = phraseHeadToHead(headToHeadData.getTeamsLastFixtures());

        return """
                Upcoming %s match: %s (home) vs %s (away)
                
                %s
                
                League standings:
                %s
                %s
                
                %s recent form:
                %s
                
                %s recent form:
                %s
                
                Head-to-head history:
                %s""".formatted(competition, home, away, coaches, homeStanding, awayStanding,
                home, homeForm, away, awayForm, h2h);
    }

    private String getLanguageDescription() {
        return switch (language) {
            case HEBREW -> "Hebrew";
            case AMERICAN -> "American English";
            case BRITISH -> "British English";
        };
    }

    private String phraseStanding(String teamName, TeamData teamData) {
        Standing standing = teamData.getStandings() != null
                ? teamData.getStandings().get(fixture.getCompetition())
                : null;

        if (standing == null) {
            return "%s: standing data unavailable".formatted(teamName);
        }

        WinLossDraw overall = standing.getOverall();
        return "%s: position %d, %d pts (%dW %dD %dL)".formatted(
                teamName,
                standing.getPosition(),
                standing.getPoints(),
                overall.getWins(),
                overall.getDraws(),
                overall.getLosses());
    }

    private String phraseRecentForm(List<Fixture> recentFixtures) {
        if (recentFixtures == null || recentFixtures.isEmpty()) {
            return "No recent fixtures available.";
        }
        return recentFixtures.stream()
                .limit(5)
                .map(f -> {
                    Score score = f.getScore();
                    String result = score.isDraw() ? "Draw" : "Win for " + score.getWinner();
                    return "  %s %d-%d %s (%s)".formatted(
                            f.getHomeTeam().getName(),
                            score.getHome(),
                            score.getAway(),
                            f.getAwayTeam().getName(),
                            result);
                })
                .collect(Collectors.joining("\n"));
    }

    private String phraseHeadToHead(List<Fixture> h2hFixtures) {
        if (h2hFixtures == null || h2hFixtures.isEmpty()) {
            return "No head-to-head history available.";
        }
        return h2hFixtures.stream()
                .limit(5)
                .map(f -> {
                    Score score = f.getScore();
                    String result = score.isDraw() ? "Draw" : "Win for " + score.getWinner();
                    return "  %s %d-%d %s (%s)".formatted(
                            f.getHomeTeam().getName(),
                            score.getHome(),
                            score.getAway(),
                            f.getAwayTeam().getName(),
                            result);
                })
                .collect(Collectors.joining("\n"));
    }
}
