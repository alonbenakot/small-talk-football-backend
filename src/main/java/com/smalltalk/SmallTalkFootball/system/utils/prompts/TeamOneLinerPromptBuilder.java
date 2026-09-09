package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.WinLossDraw;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The team-flavoured one-liner prompt: the same facts spoken by a fan, a rival fan or a
 * neutral (plan §3). Only role, style and examples differ between the three — the data block
 * is identical, because a perspective is a voice, not a different set of facts.
 * <p>
 * Stale training-data knowledge is the dominant failure mode here: the model has opinions
 * about every big club that may be years out of date, so {@link #constraints()} is firmer
 * than in the fixture builders.
 */
public class TeamOneLinerPromptBuilder implements PromptBuilder {

    private final TeamPromptContext context;
    private final Language language;
    private final Perspective perspective;

    public TeamOneLinerPromptBuilder(TeamPromptContext context, Language language, Perspective perspective) {
        this.context = context;
        this.language = language;
        this.perspective = perspective == null ? Perspective.NEUTRAL : perspective;
    }

    private String teamName() {
        String name = context.team().getName();
        return name == null || name.isBlank() ? "this team" : name;
    }

    @Override
    public String role() {
        return switch (perspective) {
            case FAN -> "You support %s and you're talking about your club with friends."
                    .formatted(teamName());
            case RIVAL_FAN -> "You support a rival club and you're winding up a %s fan."
                    .formatted(teamName());
            case NEUTRAL -> "You follow football closely and you're making conversation about %s."
                    .formatted(teamName());
        };
    }

    @Override
    public String task() {
        return "Generate a casual comment that shows you follow this team closely at the moment.";
    }

    @Override
    public String style() {
        String tone = switch (perspective) {
            case FAN -> "partisan and optimistic, forgiving of bad results";
            case RIVAL_FAN -> "teasing and needling, seizing on any weakness in the data";
            case NEUTRAL -> "observational and even-handed, with no allegiance";
        };
        return "%s, %s, casual friendly banter.".formatted(language.getDescription(), tone);
    }

    @Override
    public String structure() {
        return "1-2 sentences, under 20 words each, no line breaks, no emojis.";
    }

    @Override
    public String constraints() {
        String base = """
                Use only the data provided. No predictions, no invented transfers, injuries, quotes or statistics.
                Do not mention anything you know about this club that is not listed below.""";

        if (perspective == Perspective.RIVAL_FAN) {
            return base + """
                    
                    Mock only what is in the data. If the data is all positive, be grudging rather than inventing a flaw.
                    Do not name your own club or any specific rival.""";
        }
        return base;
    }

    @Override
    public String examples() {
        return switch (perspective) {
            case FAN -> """
                    1. Four on the bounce and top of the table, nobody's stopping us right now.
                    2. Three draws is nothing, we're still unbeaten and the run of games is kind.
                    3. Saka's carrying the front line, three goals and two assists already.""";
            case RIVAL_FAN -> """
                    1. Second is still second, and they've drawn three of five. Bottling it again.
                    2. Fine, they're top, but they've played everyone at home so far.
                    3. Their best defender is injured and it's Man City away next. Good luck with that.""";
            case NEUTRAL -> """
                    1. Arsenal are top on thirteen points, unbeaten but with three draws.
                    2. They've won four of five and travel to City on Saturday.
                    3. Their leading scorer is third in the league charts with seven.""";
        };
    }

    @Override
    public String data() {
        TeamData team = context.team();
        String name = teamName();

        return """
                Team: %s (%s)
                Coach: %s
                
                League standing:
                %s
                %s
                
                Recent form (most recent first):
                %s
                
                Next fixture:
                %s
                
                Notable players:
                %s""".formatted(
                name,
                context.competition(),
                team.getCoach() == null ? "unknown" : team.getCoach(),
                PromptPhrasing.phraseStanding(name, team, context.competition()),
                phraseHomeAwaySplit(team),
                PromptPhrasing.phraseRecentForm(context.recentForm()),
                phraseNextFixture(name),
                phrasePlayers());
    }

    private String phraseHomeAwaySplit(TeamData team) {
        Standing standing = team.getStandings() == null ? null : team.getStandings().get(context.competition());
        if (standing == null || standing.getHome() == null || standing.getAway() == null) {
            return "  Home/away split unavailable.";
        }
        return "  Home: %s | Away: %s".formatted(phraseRecord(standing.getHome()), phraseRecord(standing.getAway()));
    }

    private static String phraseRecord(WinLossDraw record) {
        return "%dW %dD %dL".formatted(nz(record.getWins()), nz(record.getDraws()), nz(record.getLosses()));
    }

    private String phraseNextFixture(String name) {
        Fixture next = context.nextFixture();
        if (next == null) {
            return "  No upcoming fixture scheduled.";
        }
        boolean atHome = next.getHomeTeam() != null && name.equals(next.getHomeTeam().getName());
        String opponent = atHome
                ? nameOf(next.getAwayTeam())
                : nameOf(next.getHomeTeam());
        return "  %s %s (%s), %s".formatted(
                atHome ? "at home to" : "away at",
                opponent,
                next.getCompetition(),
                next.getMatchDateTime());
    }

    private static String nameOf(Team team) {
        return team == null || team.getName() == null ? "an unnamed opponent" : team.getName();
    }

    /**
     * Only the 0-3 players who actually earn a mention reach the prompt; the other seven on
     * the card are there for the frontend, not the sentence.
     */
    private String phrasePlayers() {
        List<PlayerData> selected = PromptPlayerSelection.select(context.notablePlayers());
        if (selected.isEmpty()) {
            return "  No player is standing out; talk about the table and the form instead.";
        }
        return selected.stream().map(TeamOneLinerPromptBuilder::phrasePlayer).collect(Collectors.joining("\n"));
    }

    private static String phrasePlayer(PlayerData player) {
        StringBuilder line = new StringBuilder("  %s (%s)".formatted(player.getName(), player.getPosition()));
        line.append(", %d apps".formatted(nz(player.getMatchesPlayed())));
        line.append(", %d goals, %d assists".formatted(nz(player.getGoals()), nz(player.getAssists())));
        if (player.getLeagueScorerRank() != null) {
            line.append(", %d in the league scoring charts".formatted(player.getLeagueScorerRank()));
        }
        if (player.getSaves() != null) {
            line.append(", %d saves, %d conceded".formatted(nz(player.getSaves()), nz(player.getGoalsConceded())));
        }
        if (player.isInjured()) {
            line.append(", currently injured");
        }
        return line.toString();
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
