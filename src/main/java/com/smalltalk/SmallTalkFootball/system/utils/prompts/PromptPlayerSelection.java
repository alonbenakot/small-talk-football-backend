package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Narrows the ten notable players on the card down to the 0-3 worth naming in the sentence
 * (plan §6.4). A name with no story attached makes a one-liner worse, not better, so a player
 * reaches the prompt only when one of three things is true, taken in this order:
 *
 * <ol>
 *   <li><b>an injured regular</b> — the most conversation-worthy fact a squad holds, and the
 *       one a casual fan is least likely to know;</li>
 *   <li><b>a league scoring-charts place</b> from {@code get_topscorers}, best rank first —
 *       "third in the scoring charts" sounds like real knowledge;</li>
 *   <li><b>a standout contribution</b> — clearly ahead of the rest of the squad, not merely
 *       top of it. A leading scorer with three in twenty is not carrying anyone.</li>
 * </ol>
 *
 * Returning nothing is a normal outcome, and the builder falls back to a table-and-form
 * sentence.
 */
final class PromptPlayerSelection {

    static final int MAX_PROMPT_PLAYERS = 3;

    /** A standout must beat the next best by this much, or score at the rate below. */
    private static final double STANDOUT_MULTIPLE = 1.5;
    private static final double STANDOUT_RATE_PER_APPEARANCE = 0.5;

    private PromptPlayerSelection() {
    }

    static List<PlayerData> select(List<PlayerData> notablePlayers) {
        if (notablePlayers == null || notablePlayers.isEmpty()) {
            return List.of();
        }

        List<PlayerData> selected = new ArrayList<>();

        injuredRegulars(notablePlayers).forEach(player -> add(selected, player));
        leagueScorers(notablePlayers).forEach(player -> add(selected, player));
        standoutContributor(notablePlayers).ifPresent(player -> add(selected, player));

        return List.copyOf(selected);
    }

    private static void add(List<PlayerData> selected, PlayerData player) {
        if (selected.size() < MAX_PROMPT_PLAYERS && !selected.contains(player)) {
            selected.add(player);
        }
    }

    private static List<PlayerData> injuredRegulars(List<PlayerData> players) {
        int medianAppearances = medianAppearances(players);
        return players.stream()
                .filter(PlayerData::isInjured)
                .filter(player -> nz(player.getMatchesPlayed()) >= medianAppearances)
                .sorted(Comparator.comparingInt((PlayerData player) -> nz(player.getMatchesPlayed())).reversed())
                .toList();
    }

    private static List<PlayerData> leagueScorers(List<PlayerData> players) {
        return players.stream()
                .filter(player -> player.getLeagueScorerRank() != null)
                .sorted(Comparator.comparingInt(PlayerData::getLeagueScorerRank))
                .toList();
    }

    private static Optional<PlayerData> standoutContributor(List<PlayerData> players) {
        List<PlayerData> byContribution = players.stream()
                .sorted(Comparator.comparingDouble(PromptPlayerSelection::contribution).reversed())
                .toList();

        PlayerData best = byContribution.get(0);
        double bestContribution = contribution(best);
        if (bestContribution <= 0) {
            return Optional.empty();
        }

        double runnerUp = byContribution.size() > 1 ? contribution(byContribution.get(1)) : 0;
        int appearances = nz(best.getMatchesPlayed());
        boolean prolific = appearances > 0
                && (nz(best.getGoals()) + nz(best.getAssists())) / (double) appearances > STANDOUT_RATE_PER_APPEARANCE;

        return bestContribution >= runnerUp * STANDOUT_MULTIPLE || prolific
                ? Optional.of(best)
                : Optional.empty();
    }

    private static int medianAppearances(List<PlayerData> players) {
        List<Integer> appearances = players.stream()
                .map(player -> nz(player.getMatchesPlayed()))
                .sorted()
                .toList();
        return appearances.get(appearances.size() / 2);
    }

    private static double contribution(PlayerData player) {
        return 2.0 * nz(player.getGoals()) + nz(player.getAssists());
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
