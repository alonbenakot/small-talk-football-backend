package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Standing;
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
}
