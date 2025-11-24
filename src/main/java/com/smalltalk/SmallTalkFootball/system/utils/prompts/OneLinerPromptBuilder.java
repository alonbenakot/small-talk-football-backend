package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Statistic;
import com.smalltalk.SmallTalkFootball.models.Team;

import java.util.List;
import java.util.stream.Collectors;

public class OneLinerPromptBuilder implements PromptBuilder {

    private Fixture fixture;
    private Language language;
    private String preferredTeam;

    public OneLinerPromptBuilder(Fixture fixture, TeamType teamType, Language language) {
        this.fixture = fixture;
        this.language = language;
        setPreferredTeam(teamType);

    }

    public void setPreferredTeam(TeamType teamType) {
        this.preferredTeam = teamType == null
                ? ""
                : teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
    }

    @Override
    public String role() {
        return """
                You are a %s fan chatting with your friends about last night's football (soccer) match.
                """.formatted(preferredTeam);
    }

    @Override
    public String task() {
        return "Generate a casual fan comment reacting to the match.";
    }

    @Override
    public String style() {
        return "%s , slightly biased toward %s, casual friendly banter.".formatted(getLanguageDescription(), preferredTeam);
    }

    @Override
    public String structure() {
        return "1-2 sentences, under 20 words each, no line breaks, no emojis.";
    }

    @Override
    public String constraints() {
        return "No match reports, no invented events, no repeated stats or moments.";
    }

    @Override
    public String examples() {
        return """
                1. City destroyed United 3-0, but still had passed around the goal way too much.
                2. That Haaland, he's a monster. No one can stop him.
                3. Its nice that Brentford took 20 shots on goal, but when non of them hit the back of the net it doesn't really matter.
                4. City always holds the ball forever but the point is to score goals, not hold the ball.""";
    }

    @Override
    public String data() {
        Competition competition = fixture.getCompetition();
        String home = fixture.getHomeTeam().getName();
        String away = fixture.getAwayTeam().getName();
        Score score = fixture.getScore();
        String goals = phraseGoals(fixture.getGoals(), fixture.getHomeTeam(), fixture.getAwayTeam());
        String stats = phraseStatistics(fixture.getStatistics());
        String winner = getWinner();
        String coaches = getCoaches(fixture);

        String prompt = """
            This is a %s match between %s (home) and %s (away).

            Final score:
            %s - %s
            %s - %s

            %s
            
            %s

            These are the goals by chronological order:
            %s

            %s""";

        return prompt.formatted(competition, home, away, home, score.getHome(), away, score.getAway(), coaches, winner, goals, stats);
    }

    private String getLanguageDescription() {
        return switch (language) {
            case HEBREW -> "Hebrew";
            case AMERICAN -> "American English";
            case BRITISH -> "British English";
        };
    }

    private String getCoaches(Fixture fixture) {
        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();
        return """
                %s coach: %s
                %s coach: %s"""
                .formatted(homeTeam.getName(), homeTeam.getCoach(), awayTeam.getName(), awayTeam.getCoach());
    }


    private String getWinner() {
        return fixture.getScore().isDraw()
                ? "Draw"
                : "Winner - " + fixture.getScore().getWinner();
    }

    private static String phraseStatistics(List<Statistic> statistics) {
        String statsBody = statistics.stream()
                .map(stat -> """
                    %s
                    Home team - %s%s
                    Away team - %s%s
                    """.formatted(
                        stat.getType(),
                        stat.getHome(), stat.isPercentage() ? "%" : "",
                        stat.getAway(), stat.isPercentage() ? "%" : ""
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        return """
            These are the match statistics:
            %s""".formatted(statsBody);
    }


    private static String phraseGoals(List<Goal> goals, Team homeTeam, Team awayTeam) {
        return goals.stream()
                .map(goal -> {
                    String assistLine = (goal.getAssistBy() != null && !goal.getAssistBy().isBlank())
                            ? "Assist by %s%n".formatted(goal.getAssistBy())
                            : "";

                    return """
                        
                        At the minute: %s
                        Goal by %s for %s
                        %sPuts the score at: %s for %s and %s for %s
                        """.formatted(
                            goal.getMinute(),
                            goal.getGoalBy(),
                            goal.getTeamName(),
                            assistLine,
                            goal.getHomeScore(), homeTeam.getName(),
                            goal.getAwayScore(), awayTeam.getName()
                    );
                })
                .collect(Collectors.joining());
    }


}
