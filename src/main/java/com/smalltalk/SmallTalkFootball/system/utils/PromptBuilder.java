package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Team;

import java.util.List;

public class PromptBuilder {

    private static final String DELIMITER = "######################################";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String EXAMPLE = """
            Did you see that ludicrous display last night?
            What was Wenger thinking?
            The thing about Arsenal is, they always try an' walk it in.
            """;

    public static String forOneliner(Fixture fixture, TeamType teamType, Language lang) {
        String preferredTeam = teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
        return new StringBuilder()
                .append("You are a witty ").append(preferredTeam).append(" fan chatting with your mate about last night's match.")
                .append(LINE_SEPARATOR)
                .append(phraseTeamPreference(fixture, teamType))
                .append("Generate a casual, cheeky comment (1-2 sentences max) that a fan would actually say to impress their friends. Think pub banter, not match reports.")
                .append(LINE_SEPARATOR)
                .append("Use ").append(lang).append("football slang and attitude. Be a bit biased toward").append(preferredTeam)
                .append(LINE_SEPARATOR)
                .append("Examples of the tone you want:")
                .append(LINE_SEPARATOR)
                .append(EXAMPLE)
                .append("Match data: ")
                .append(LINE_SEPARATOR)
                .append(DELIMITER)
                .append(LINE_SEPARATOR)
                .append(phraseMatchData(fixture))
                .append(DELIMITER)
                .append("Generate ONLY the casual fan comment - no explanations or context.")
                .toString();
    }

    private static String phraseTeamPreference(Fixture fixture, TeamType teamType) {
        String teamName = teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
        if (teamType != null) {
            return String.format("Your are a %s fan. Like all fans, you might be a little biased toward your team." + LINE_SEPARATOR, teamName);
        }
        return "";
    }

    private static String phraseMatchData(Fixture fixture) {
        return new StringBuilder()
                .append("This is a ").append(fixture.getCompetition().toString()).append(" match between ")
                .append(fixture.getHomeTeam().getName()).append(" (home) ")
                .append("and ").append(fixture.getAwayTeam().getName()).append(" (away)")
                .append(LINE_SEPARATOR)
                .append("The match is being held at ").append(fixture.getVenue())
                .append(LINE_SEPARATOR)
                .append("Final score: ")
                .append(LINE_SEPARATOR)
                .append(fixture.getHomeTeam().getName()).append(" - ").append(fixture.getScore().getHome())
                .append(LINE_SEPARATOR)
                .append(fixture.getAwayTeam().getName()).append(" - ").append(fixture.getScore().getAway())
                .append("Winner - ").append(fixture.getScore().getWinner())
                .append("These are the goals by chronological order: ")
                .append(LINE_SEPARATOR)
                .append(phraseGoals(fixture.getGoals(), fixture.getHomeTeam(), fixture.getAwayTeam()))
                .append(LINE_SEPARATOR)
                .toString();

    }

    private static String phraseGoals(List<Goal> goals, Team homeTeam, Team awayTeam) {
        StringBuilder goalsSummary = new StringBuilder();
        goals.forEach(goal -> goalsSummary
                .append(LINE_SEPARATOR)
                .append("At the minute: ").append(goal.getMinute())
                .append(LINE_SEPARATOR)
                .append("Goal by ").append(goal.getGoalBy()).append(" for ").append(goal.getTeamName())
                .append(LINE_SEPARATOR)
                .append("Assist by ").append(goal.getAssistBy())
                .append(LINE_SEPARATOR)
                .append("Puts the score at: ")
                .append(goal.getHomeScore()).append(" for ").append(homeTeam.getName())
                .append(" and ").append(goal.getAwayScore()).append(" for ").append(awayTeam.getName())
        );

        return goalsSummary.toString();
    }
}
