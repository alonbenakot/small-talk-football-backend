package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ten notable players are for the card; the sentence gets at most three, and only when
 * they have a story attached (plan §6.4). Naming a player whose only distinction is being the
 * least bad in a poor squad is exactly the empty stat the prompt's constraints exist to
 * suppress, so "nobody qualifies" has to be a normal outcome.
 */
class PromptPlayerSelectionTest {

    private static PlayerData.PlayerDataBuilder player(String name, int apps, int goals, int assists) {
        return PlayerData.builder()
                .id(name).teamId("2621").name(name).position("Forwards")
                .matchesPlayed(apps).goals(goals).assists(assists);
    }

    private static List<String> namesFrom(List<PlayerData> squad) {
        return PromptPlayerSelection.select(squad).stream().map(PlayerData::getName).toList();
    }

    @Test
    void selectsNobodyFromAnEmptyOrNullList() {
        assertThat(PromptPlayerSelection.select(List.of())).isEmpty();
        assertThat(PromptPlayerSelection.select(null)).isEmpty();
    }

    /**
     * An injured regular is the most conversation-worthy fact a squad holds, and the one a
     * casual fan is least likely to know — so it outranks a bigger scorer who is fit.
     */
    @Test
    void anInjuredRegularComesBeforeAHigherScorer() {
        List<PlayerData> squad = List.of(
                player("fit-scorer", 10, 9, 3).build(),
                player("injured-regular", 10, 2, 1).injured(true).build(),
                player("fringe", 2, 0, 0).build());

        assertThat(namesFrom(squad)).first().isEqualTo("injured-regular");
    }

    @Test
    void anInjuredSquadPlayerWhoBarelyPlaysIsNotNotable() {
        List<PlayerData> squad = List.of(
                player("regular-a", 10, 1, 1).build(),
                player("regular-b", 10, 1, 1).build(),
                player("regular-c", 10, 1, 1).build(),
                player("injured-fringe", 1, 0, 0).injured(true).build());

        assertThat(namesFrom(squad)).doesNotContain("injured-fringe");
    }

    /**
     * A league scoring-charts place is a fact about the league, not just about the squad, so
     * it beats a teammate whose raw numbers happen to be higher within the team.
     */
    @Test
    void aLeagueScorerRankBeatsRawGoals() {
        List<PlayerData> squad = List.of(
                player("raw-goals", 10, 8, 0).build(),
                player("ranked", 10, 6, 0).leagueScorerRank(2).build());

        assertThat(namesFrom(squad)).first().isEqualTo("ranked");
    }

    @Test
    void betterLeagueRanksComeFirst() {
        List<PlayerData> squad = List.of(
                player("tenth", 10, 5, 0).leagueScorerRank(10).build(),
                player("second", 10, 4, 0).leagueScorerRank(2).build());

        assertThat(namesFrom(squad)).containsExactly("second", "tenth");
    }

    /**
     * Three goals in twenty games makes somebody the leading scorer of a poor side, not a
     * player carrying anyone.
     */
    @Test
    void aMerelyTopOfAPoorSquadScorerQualifiesForNothing() {
        List<PlayerData> squad = List.of(
                player("leading-scorer", 20, 3, 0).build(),
                player("next-best", 20, 2, 2).build(),
                player("third", 20, 1, 2).build());

        assertThat(PromptPlayerSelection.select(squad)).isEmpty();
    }

    @Test
    void aClearStandoutQualifies() {
        List<PlayerData> squad = List.of(
                player("standout", 10, 9, 4).build(),
                player("next-best", 10, 1, 1).build(),
                player("third", 10, 0, 1).build());

        assertThat(namesFrom(squad)).containsExactly("standout");
    }

    @Test
    void neverNamesMoreThanThree() {
        List<PlayerData> squad = List.of(
                player("injured-one", 10, 1, 1).injured(true).build(),
                player("injured-two", 10, 1, 1).injured(true).build(),
                player("ranked-one", 10, 6, 1).leagueScorerRank(1).build(),
                player("ranked-two", 10, 5, 1).leagueScorerRank(4).build(),
                player("standout", 10, 12, 6).build());

        assertThat(PromptPlayerSelection.select(squad)).hasSize(3);
    }

    @Test
    void doesNotNameTheSamePlayerTwice() {
        List<PlayerData> squad = List.of(
                player("injured-and-ranked", 10, 9, 3).injured(true).leagueScorerRank(1).build(),
                player("filler", 10, 0, 0).build());

        assertThat(namesFrom(squad)).containsExactly("injured-and-ranked");
    }
}
