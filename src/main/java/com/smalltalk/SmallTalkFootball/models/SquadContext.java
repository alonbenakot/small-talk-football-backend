package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;

import java.util.Comparator;
import java.util.List;

/**
 * Where a player sits in his own squad (plan §4.3) — computed from the stored squad, never
 * stored itself. "Eight goals" means nothing on its own; "their leading scorer" does. Both the
 * card and the prompt read these same numbers.
 *
 * @param leadingScorer      the highest {@code goals} in the squad, and at least one
 * @param leadingContributor clearly ahead of the rest on {@code goals * 2 + assists}: beating
 *                           the runner-up by {@value #STANDOUT_MULTIPLE}x, or contributing at
 *                           over {@value #STANDOUT_RATE_PER_APPEARANCE} per appearance
 * @param everPresent        appearances at or near the squad's maximum
 * @param firstChoiceKeeper  a {@code Goalkeepers} entry with the most appearances of the squad's keepers
 * @param appearanceShare    his appearances against the squad's maximum, 0 when nobody has played
 * @param squadSize          how many players the squad holds
 */
public record SquadContext(boolean leadingScorer,
                           boolean leadingContributor,
                           boolean everPresent,
                           boolean firstChoiceKeeper,
                           double appearanceShare,
                           int squadSize) {

    // Tuned in the Phase 4 smoke. The contributor thresholds are the ones PromptPlayerSelection
    // already uses for the team sentence.
    static final double STANDOUT_MULTIPLE = 1.5;
    static final double STANDOUT_RATE_PER_APPEARANCE = 0.5;
    static final double EVER_PRESENT_SHARE = 0.9;
    private static final String GOALKEEPERS = "Goalkeepers";

    public static SquadContext of(PlayerData player, List<PlayerData> squad) {
        int maxAppearances = squad.stream().mapToInt(p -> nz(p.getMatchesPlayed())).max().orElse(0);
        double share = maxAppearances == 0 ? 0 : (double) nz(player.getMatchesPlayed()) / maxAppearances;
        // Compared by id, not reference: the player and the squad come from separate queries.
        PlayerData keeper = firstChoiceKeeper(squad);

        return new SquadContext(
                isLeadingScorer(player, squad),
                isLeadingContributor(player, squad),
                share >= EVER_PRESENT_SHARE,
                keeper != null && keeper.getId().equals(player.getId()),
                share,
                squad.size());
    }

    private static boolean isLeadingScorer(PlayerData player, List<PlayerData> squad) {
        int goals = nz(player.getGoals());
        return goals > 0 && squad.stream().allMatch(p -> nz(p.getGoals()) <= goals);
    }

    private static boolean isLeadingContributor(PlayerData player, List<PlayerData> squad) {
        double own = contribution(player);
        if (own <= 0 || squad.stream().anyMatch(p -> contribution(p) > own)) {
            return false;
        }
        double runnerUp = squad.stream()
                .filter(p -> p != player)
                .mapToDouble(SquadContext::contribution)
                .max()
                .orElse(0);
        int appearances = nz(player.getMatchesPlayed());
        boolean prolific = appearances > 0
                && (nz(player.getGoals()) + nz(player.getAssists())) / (double) appearances > STANDOUT_RATE_PER_APPEARANCE;
        return own >= runnerUp * STANDOUT_MULTIPLE || prolific;
    }

    /** The {@code Goalkeepers} entry with the most appearances, or the first one when none has played. */
    public static PlayerData firstChoiceKeeper(List<PlayerData> squad) {
        return squad.stream()
                .filter(p -> GOALKEEPERS.equalsIgnoreCase(p.getPosition()))
                .max(Comparator.comparingInt(p -> nz(p.getMatchesPlayed())))
                .orElse(null);
    }

    private static double contribution(PlayerData player) {
        return 2.0 * nz(player.getGoals()) + nz(player.getAssists());
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
