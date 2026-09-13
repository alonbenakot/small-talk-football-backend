package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.models.MatchContribution;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PlayerAngleSelection.Angle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The player one-liner prompt: one voice, no perspective (plan §3). The data block leads with
 * the angle {@link PlayerAngleSelection} picked and carries the club context for every angle,
 * so even a player who has not featured gets something worth saying.
 * <p>
 * The constraints are the firmest of the four builders. The model knows a great deal about
 * famous footballers and much of it is out of date; for a named individual a stale memory
 * becomes a confidently wrong claim about a real person (plan §6.2). Absent stats are phrased
 * as absent, never as zero — a blank in the feed means "not recorded".
 */
public class PlayerOneLinerPromptBuilder implements PromptBuilder {

    private final PlayerPromptContext context;
    private final Language language;

    public PlayerOneLinerPromptBuilder(PlayerPromptContext context, Language language) {
        this.context = context;
        this.language = language;
    }

    private PlayerData player() {
        return context.player();
    }

    private String playerName() {
        String name = player().getName();
        return name == null || name.isBlank() ? "this player" : name;
    }

    private String clubName() {
        TeamData team = context.team();
        String name = team == null ? player().getTeamName() : team.getName();
        return name == null || name.isBlank() ? "his club" : name;
    }

    @Override
    public String role() {
        return "You follow football closely and you're making conversation about %s of %s."
                .formatted(playerName(), clubName());
    }

    @Override
    public String task() {
        return "Generate a casual comment that shows you know how this player is doing right now.";
    }

    @Override
    public String style() {
        return "%s, observational, casual friendly banter.".formatted(language.getDescription());
    }

    @Override
    public String structure() {
        return "1-2 sentences, under 20 words each, no line breaks, no emojis.";
    }

    @Override
    public String constraints() {
        return """
                Use only the data provided.
                Do not mention this player's nationality, age, former clubs, transfer value, honours or career history unless it appears in the data below.
                No predictions, no invented transfers, injuries, quotes or statistics.
                Do not speculate about his future, his attitude, his fitness or his relationship with the club.
                Do not describe the nature of an injury or how it happened.
                Do not say he scored, assisted or featured in any particular match unless that match is listed under his goals and assists.
                Put it in your own words - do not copy phrases from the data.
                A stat marked "not recorded" is unknown, not zero - do not mention it.
                If the data is thin, say something modest and true rather than reaching for something you remember.""";
    }

    @Override
    public String examples() {
        return """
                1. Haaland's eight in ten and top of the charts, City are just feeding him.
                2. Alisson's up to twenty-one saves and they've still let in twelve, he's being left exposed.
                3. Van Dijk hasn't missed a game at the back for a side sitting second.
                4. Jota's out injured, and they'll miss him, he'd started nine of ten.
                5. Elliott's barely featured, three appearances all season, mind you they're top and unchanged.
                6. Ramsay is yet to play a minute this season, hard to say much beyond that.""";
    }

    @Override
    public String data() {
        PlayerData player = player();
        SquadContext squad = context.squadContext();

        return """
                Player: %s
                Fitness: %s
                Season so far: %s
                In the squad: %s
                Lead with: %s
                %s
                %s""".formatted(
                phrasePlayer(player),
                player.isInjured() ? "currently injured" : "fit",
                phraseSeason(player),
                phraseSquad(squad),
                phraseAngle(PlayerAngleSelection.select(player, squad), player),
                phraseContributions(),
                phraseClub());
    }

    /**
     * Left out entirely when there is nothing on record. The live smoke showed that any wording
     * of the empty case ("no goals or assists on record") is read as zero, and a fixture ingested
     * before the scorer ids were bound contributes nothing even if he scored in it (plan §4.4).
     */
    private String phraseContributions() {
        if (context.recentContributions().isEmpty()) {
            return "";
        }
        return """

                His goals and assists in the club's recent games (most recent first):
                %s
                """.formatted(context.recentContributions().stream()
                .map(c -> "  %d goals, %d assists v %s (%s)".formatted(c.goals(), c.assists(), c.opponent(), c.date()))
                .collect(Collectors.joining("\n")));
    }

    private String phrasePlayer(PlayerData player) {
        StringBuilder line = new StringBuilder(playerName());
        line.append(", %s, %s".formatted(orUnknown(player.getPosition()), clubName()));
        if (player.getNumber() != null && !player.getNumber().isBlank()) {
            line.append(", shirt ").append(player.getNumber());
        }
        if (player.getAge() != null && !player.getAge().isBlank()) {
            line.append(", age ").append(player.getAge());
        }
        if (player.isCaptain()) {
            line.append(", captain");
        }
        return line.toString();
    }

    private static String phraseSeason(PlayerData player) {
        StringBuilder line = new StringBuilder("%s apps, %s goals, %s assists".formatted(
                stat(player.getMatchesPlayed()), stat(player.getGoals()), stat(player.getAssists())));
        if (player.getRating() != null && !player.getRating().isBlank()) {
            line.append(", rating ").append(player.getRating());
        }
        if (player.getLeagueScorerRank() != null) {
            line.append(", %d in the league scoring charts".formatted(player.getLeagueScorerRank()));
        }
        if (player.getSaves() != null) {
            line.append(", %d saves, %s conceded".formatted(player.getSaves(), stat(player.getGoalsConceded())));
        }
        return line.toString();
    }

    /**
     * Flags only. The squad size and appearance share both leaked through in the smoke ("in a
     * 32-man squad", "40% share of the busiest guy's minutes"); the angle says where he stands.
     * Being first-choice keeper is not a flag either: it is the baseline for a keeper, and naming
     * it produced "clearly City's first-choice, nine games already".
     */
    private static String phraseSquad(SquadContext squad) {
        List<String> flags = new ArrayList<>();
        if (squad.leadingScorer()) {
            flags.add("the squad's leading scorer");
        }
        if (squad.leadingContributor()) {
            flags.add("clearly the squad's leading goal contributor");
        }
        return flags.isEmpty() ? "nothing stands out" : String.join(", ", flags);
    }

    /**
     * Topic phrases, not sentences: the Phase 4 smoke showed a sentence here gets copied into
     * the answer word for word ("He is a regular who is currently injured and being missed").
     */
    private static String phraseAngle(Angle angle, PlayerData player) {
        return switch (angle) {
            case INJURED -> "his injury - a regular starter the side is currently without";
            case LEAGUE_SCORER -> "his place in the league scoring charts";
            case LEADING_CONTRIBUTOR -> "his share of the squad's goals and assists";
            case KEEPER -> "how busy he is - his saves against what the side concedes; being first choice and playing every game is a given for a keeper, do not remark on either";
            case EVER_PRESENT -> "his reliability - he plays nearly every game - rather than his numbers";
            case REGULAR -> "his role as a regular starter with nothing standing out; stay modest and lean on the club's situation";
            case FRINGE -> "how little he has featured (%d appearances); plain, not flattering"
                    .formatted(player.getMatchesPlayed());
            case UNUSED -> "that he has not played this season; plain, and lean on the club's situation";
        };
    }

    private String phraseClub() {
        TeamData team = context.team();
        if (team == null) {
            return "Club: %s, no further club data available - talk about the player alone.".formatted(clubName());
        }
        return """
                Club: %s
                Coach: %s

                League standing:
                %s

                Recent form (most recent first):
                %s

                Next fixture:
                %s""".formatted(
                clubName(),
                orUnknown(team.getCoach()),
                context.competition() == null
                        ? "%s: standing data unavailable".formatted(clubName())
                        : PromptPhrasing.phraseStanding(clubName(), team, context.competition()),
                PromptPhrasing.phraseRecentForm(context.recentForm()),
                PromptPhrasing.phraseNextFixture(context.nextFixture(), team.getId()));
    }

    private static String stat(Integer value) {
        return value == null ? "not recorded" : value.toString();
    }

    private static String orUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
