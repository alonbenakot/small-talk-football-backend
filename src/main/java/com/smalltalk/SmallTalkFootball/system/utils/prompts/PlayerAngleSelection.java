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
        /** Appearances well below the squad median, but above zero. */
        FRINGE,
        /** No appearances at all. */
        UNUSED
    }

    // Starting points, to be tuned from the Phase 4 smoke.
    private static final int MAX_LEAGUE_SCORER_RANK = 5;
    /** A fringe player has fewer than this share of the squad's median appearances. */
    private static final double FRINGE_SHARE_OF_MEDIAN = 0.5;

    private PlayerAngleSelection() {
    }

    static Angle select(PlayerData player, SquadContext squad) {
        int appearances = player.getMatchesPlayed() == null ? 0 : player.getMatchesPlayed();

        if (player.isInjured() && appearances > 0 && appearances >= squad.medianAppearances()) {
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
        if (appearances < squad.medianAppearances() * FRINGE_SHARE_OF_MEDIAN) {
            return Angle.FRINGE;
        }
        return Angle.REGULAR;
    }
}
