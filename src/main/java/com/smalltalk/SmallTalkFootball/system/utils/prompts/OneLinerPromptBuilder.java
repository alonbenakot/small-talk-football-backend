package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Statistic;
import com.smalltalk.SmallTalkFootball.models.Team;

import java.util.List;

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
        return "%s english , slightly biased toward %s, casual friendly banter.".formatted(language, preferredTeam);
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
        return phraseMatchData(fixture);
    }

    private static final String DELIMITER = "######################################";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String EXAMPLES = """
            1. City destroyed United 3-0, but still had passed around the goal way too much.
            2. That Haaland, he's a monster. No one can stop him.
            3. Its nice that Brentford took 20 shots on goal, but when non of them hit the back of the net it doesn't really matter.
            4. City always holds the ball forever but the point is to score goals, not hold the ball.""";

    public static String forOneliner(Fixture fixture, TeamType teamType, Language lang) {
        String preferredTeam = teamType == TeamType.HOME ? fixture.getHomeTeam().getName() : fixture.getAwayTeam().getName();
        return new StringBuilder()
                .append("You are a witty ").append(preferredTeam).append(" fan chatting with your mate about last night's football (soccer) match.")
                .append(LINE_SEPARATOR)
                .append("Generate a casual comment (1-2 sentences max) that a fan say to would say when talking to friends. Think friendly fan banter, not match reports.")
                .append(LINE_SEPARATOR)
                .append("Use ").append(lang).append("football slang and attitude. Be a bit biased toward").append(preferredTeam)
                .append("Generate ONLY the casual fan comment - no explanations or context.")
                .append(LINE_SEPARATOR)
                .append("You do not have to use every piece of data given, focus on what made an impact on the game.")
                .append(LINE_SEPARATOR)
                .append("When using statistics, you might have received data with duplicate types. Use only the one that seems the most updated.")
                .append(LINE_SEPARATOR)
                .append("I remind you that this is not an american football match, it's a soccer match. DO NOT use american football imagery.")
                .append(LINE_SEPARATOR)
                .append("Use swagger like a sports fan, but do not invent American football plays or jargon. Just attitude.")
                .append(LINE_SEPARATOR)
                .append("When talking about players, use only their last names.")
                .append(LINE_SEPARATOR)
                .append("Write in short, clear sentences. No em dashes (—), no hyphens (-) to join ideas. Just use periods or commas.")
                .append(LINE_SEPARATOR)
                .append("Write like you’re chatting with a friend in the pub or at the office: casual, clever, sometimes a bit exaggerated")
                .append(LINE_SEPARATOR)
                .append("Keep it in the style of football banter: plain-spoken, and sporty, not internet slang.")
                .append(LINE_SEPARATOR)
                .append("Any metaphors should be sports-related, not texting or online slang.")
                .append(LINE_SEPARATOR)
                .append("Maximum two sentences, each under 20 words.")
                .append(LINE_SEPARATOR)
                .append("Do not break lines at the end of sentences.")
                .append(LINE_SEPARATOR)
                .append("Use the team names, not \"we\"\\\"they\".")
                .append(LINE_SEPARATOR)
                .append("Mention each goal or match event only once. Do not rephrase or repeat the same moment in another sentence.")
                .append(LINE_SEPARATOR)
                .append("Tell the events in the order they happened — do not jump back and forth in time.")
                .append(LINE_SEPARATOR)
                .append("Do not imply extra goals or comeback moments beyond the listed ones. If the equalizer secured the result, state it clearly without suggesting another event happened.")
                .append(LINE_SEPARATOR)
                .append("DO NOT SIMPLY REPORT MATCH EVENTS, you are sharing your overall experience from the match!")
                .append(LINE_SEPARATOR)
                .append("Examples one-liners:")
                .append(LINE_SEPARATOR)
                .append(EXAMPLES)
                .append("Match data: ")
                .append(LINE_SEPARATOR)
                .append(DELIMITER)
                .append(LINE_SEPARATOR)
                .append(phraseMatchData(fixture))
                .append(DELIMITER)
                .toString();
    }

    private static String phraseMatchData(Fixture fixture) {
        return new StringBuilder()
                .append("This is a ").append(fixture.getCompetition().toString()).append(" match between ")
                .append(fixture.getHomeTeam().getName()).append(" (home) ")
                .append("and ").append(fixture.getAwayTeam().getName()).append(" (away)")
                .append(LINE_SEPARATOR)
                .append("Final score: ")
                .append(LINE_SEPARATOR)
                .append(fixture.getHomeTeam().getName()).append(" - ").append(fixture.getScore().getHome())
                .append(LINE_SEPARATOR)
                .append(fixture.getAwayTeam().getName()).append(" - ").append(fixture.getScore().getAway())
                .append(LINE_SEPARATOR)
                .append("Winner - ").append(fixture.getScore().getWinner())
                .append(LINE_SEPARATOR)
                .append("These are the goals by chronological order: ")
                .append(phraseGoals(fixture.getGoals(), fixture.getHomeTeam(), fixture.getAwayTeam()))
                .append(LINE_SEPARATOR)
                .append(phraseStatistics(fixture.getStatistics()))
                .append(LINE_SEPARATOR)
                .toString();

    }

    private static String phraseStatistics(List<Statistic> statistics) {
        StringBuilder statsSummary = new StringBuilder();
        statsSummary.append("These are the match statistics: ");
        statistics.forEach(statistic -> statsSummary
                .append(LINE_SEPARATOR)
                .append(statistic.getType())
                .append(LINE_SEPARATOR)
                .append("Home team - ").append(statistic.getHome()).append(statistic.isPercentage() ? "%" : "")
                .append(LINE_SEPARATOR)
                .append("Away team - ").append(statistic.getAway()).append(statistic.isPercentage() ? "%" : "")
        );
        return statsSummary.toString();
    }

    private static String phraseGoals(List<Goal> goals, Team homeTeam, Team awayTeam) {
        StringBuilder goalsSummary = new StringBuilder();
        goals.forEach(goal -> {
            goalsSummary
                    .append(LINE_SEPARATOR)
                    .append("At the minute: ").append(goal.getMinute())
                    .append(LINE_SEPARATOR)
                    .append("Goal by ").append(goal.getGoalBy()).append(" for ").append(goal.getTeamName())
                    .append(LINE_SEPARATOR);

            if (goal.getAssistBy() != null && !goal.getAssistBy().isBlank()) {
                goalsSummary.append("Assist by ").append(goal.getAssistBy())
                        .append(LINE_SEPARATOR);
            }

            goalsSummary
                    .append("Puts the score at: ")
                    .append(goal.getHomeScore()).append(" for ").append(homeTeam.getName())
                    .append(" and ").append(goal.getAwayScore()).append(" for ").append(awayTeam.getName());
        });
        return goalsSummary.toString();
    }

}
