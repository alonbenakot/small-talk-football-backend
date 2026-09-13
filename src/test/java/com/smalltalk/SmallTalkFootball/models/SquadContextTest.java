package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In production the player and his squad come from two separate queries, so they are never
 * the same instance. The Phase 4 smoke found {@code firstChoiceKeeper} was always false live
 * because it compared by reference; every test had passed the same object in both places.
 */
class SquadContextTest {

    private static PlayerData keeper(String id, Integer apps) {
        return PlayerData.builder().id(id).teamId("80").position("Goalkeepers").matchesPlayed(apps).build();
    }

    @Test
    void firstChoiceKeeperIsMatchedByIdNotByReference() {
        PlayerData loadedSeparately = keeper("gk-1", 9);
        List<PlayerData> squad = List.of(keeper("gk-1", 9), keeper("gk-2", null), keeper("gk-3", null));

        assertThat(SquadContext.of(loadedSeparately, squad).firstChoiceKeeper()).isTrue();
    }

    @Test
    void theBackupKeeperIsNotFirstChoice() {
        List<PlayerData> squad = List.of(keeper("gk-1", 6), keeper("gk-2", 7));

        assertThat(SquadContext.of(keeper("gk-1", 6), squad).firstChoiceKeeper()).isFalse();
        assertThat(SquadContext.of(keeper("gk-2", 7), squad).firstChoiceKeeper()).isTrue();
    }

    @Test
    void anOutfieldPlayerIsNeverTheKeeper() {
        PlayerData striker = PlayerData.builder().id("p-1").position("Forwards").matchesPlayed(10).build();

        assertThat(SquadContext.of(striker, List.of(striker, keeper("gk-1", 9))).firstChoiceKeeper()).isFalse();
    }
}
