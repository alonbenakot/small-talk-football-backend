package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.WinLossDraw;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared phrasing helpers for the one-liner prompt builders. A single team's league
 * standing and its recent-form list are needed verbatim by more than one builder, so
 * they live here rather than being copied per builder.
 */
final class PromptPhrasing {

    private PromptPhrasing() {
    }

    static String phraseStanding(String teamName, TeamData teamData, Competition competition) {
        Standing standing = teamData.getStandings() != null
                ? teamData.getStandings().get(competition)
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

    static String phraseRecentForm(List<Fixture> recentFixtures) {
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

    /**
     * Home or away is decided on team ids, never names: {@code TeamData.name} and the name on
     * a {@code Fixture} come from different apifootball endpoints and disagree ("Manchester
     * United" vs "Manchester Utd"). A name comparison silently inverts the venue and then
     * names the team as its own opponent.
     */
    static String phraseNextFixture(Fixture next, String teamId) {
        if (next == null) {
            return "  No upcoming fixture scheduled.";
        }
        boolean atHome = next.getHomeTeam() != null && teamId != null
                && teamId.equals(next.getHomeTeam().getId());
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
}
