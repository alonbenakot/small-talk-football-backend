package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.models.SquadContext;

/**
 * Decides what a sentence about one player should be about (plan §6.3) — the analogue of
 * {@link PromptPlayerSelection} one level down. The first angle that applies wins, and the
 * builder passes it to the data block as the lead fact.
 * <p>
 * The last three angles are what make the feature honest: most of a 5,500-player collection
 * is not notable, and a user is free to ask about any of them. A player is never rejected —
 * a fringe or unused player gets the plain truth rather than an error.
 */
final class PlayerAngleSelection {

    enum Angle {
        /** Injured, and a regular — the fact a casual fan is least likely to know. */
        INJURED,
        /** A top-{@value #MAX_LEAGUE_SCORER_RANK} place in the league scoring charts. */
        LEAGUE_SCORER,
        /** Clearly ahead of the squad on goal contributions. */
        LEADING_CONTRIBUTOR,
        /** The first-choice keeper, with saves on record. */
        KEEPER,
        /** Plays nearly every game without an attacking angle — the goalless centre-back. */
        EVER_PRESENT,
        /** Plays regularly, no standout number; talk about his role and the club. */
        REGULAR,
        /** Appearances well below the squad's busiest player, but above zero. */
        FRINGE,
        /** No appearances at all. */
        UNUSED
    }

    // Tuned in the Phase 4 smoke. The share is against the squad's busiest player, not the
    // median: half a squad never plays, so the median sat at 3-4 and made 3 of 12 a "regular".
    private static final int MAX_LEAGUE_SCORER_RANK = 5;
    /** At or above this appearance share a player is a regular; below it he is fringe. */
    private static final double REGULAR_SHARE = 0.5;

    private PlayerAngleSelection() {
    }

    static Angle select(PlayerData player, SquadContext squad) {
        int appearances = player.getMatchesPlayed() == null ? 0 : player.getMatchesPlayed();

        if (player.isInjured() && squad.appearanceShare() >= REGULAR_SHARE) {
            return Angle.INJURED;
        }
        if (player.getLeagueScorerRank() != null && player.getLeagueScorerRank() <= MAX_LEAGUE_SCORER_RANK) {
            return Angle.LEAGUE_SCORER;
        }
        if (squad.leadingContributor()) {
            return Angle.LEADING_CONTRIBUTOR;
        }
        if (squad.firstChoiceKeeper() && player.getSaves() != null) {
            return Angle.KEEPER;
        }
        if (appearances == 0) {
            return Angle.UNUSED;
        }
        if (squad.everPresent()) {
            return Angle.EVER_PRESENT;
        }
        if (squad.appearanceShare() < REGULAR_SHARE) {
            return Angle.FRINGE;
        }
        return Angle.REGULAR;
    }
}
