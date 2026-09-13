package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.system.utils.prompts.PlayerAngleSelection.Angle;

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
                A stat marked "not recorded" is unknown, not zero - do not mention it.
                If the data is thin, say something modest and true rather than reaching for something you remember.""";
    }

    @Override
    public String examples() {
        return """
                1. Haaland's eight in ten and top of the charts, City are just feeding him.
                2. Alisson's made twenty-one saves in nine, they're leaning on him more than they'd like.
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

                %s""".formatted(
                phrasePlayer(player),
                player.isInjured() ? "currently injured" : "fit",
                phraseSeason(player),
                phraseSquad(squad),
                phraseAngle(PlayerAngleSelection.select(player, squad), player),
                phraseClub());
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

    private static String phraseSquad(SquadContext squad) {
        StringBuilder line = new StringBuilder("squad of %d".formatted(squad.squadSize()));
        if (squad.leadingScorer()) {
            line.append(", the squad's leading scorer");
        }
        if (squad.leadingContributor()) {
            line.append(", clearly the squad's leading goal contributor");
        }
        if (squad.firstChoiceKeeper()) {
            line.append(", the first-choice goalkeeper");
        }
        line.append(", has played %d%% of the games the squad's busiest player has".formatted(
                Math.round(squad.appearanceShare() * 100)));
        return line.toString();
    }

    private static String phraseAngle(Angle angle, PlayerData player) {
        return switch (angle) {
            case INJURED -> "he is a regular who is currently injured and being missed";
            case LEAGUE_SCORER -> "his place in the league scoring charts";
            case LEADING_CONTRIBUTOR -> "he is carrying the squad's goal contributions";
            case KEEPER -> "he is the first-choice goalkeeper; his saves and what the side concedes in front of him";
            case EVER_PRESENT -> "he plays nearly every game; his reliability rather than his numbers";
            case REGULAR -> "he plays regularly but nothing in his numbers stands out; keep it modest and lean on the club's situation";
            case FRINGE -> "he has barely featured (%d appearances); say so plainly rather than making him sound important"
                    .formatted(player.getMatchesPlayed());
            case UNUSED -> "he has not played this season; say so plainly and lean on the club's situation";
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
