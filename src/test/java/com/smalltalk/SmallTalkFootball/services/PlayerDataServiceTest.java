package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.repositories.PlayerDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pins the "notable players" ranking (plan §6.4): ten for the card, the first-choice
 * goalkeeper always among them, unused squad members dropped, and a per-position cap so the
 * list is not all forwards.
 */
@ExtendWith(MockitoExtension.class)
class PlayerDataServiceTest {

    private static final String TEAM = "80";

    @Mock
    private PlayerDataRepository repository;

    private PlayerDataService service() {
        return new PlayerDataService(repository);
    }

    private static PlayerData player(String id, String position, Integer apps,
                                     int goals, int assists, int passes, int duelsWon, int shots) {
        return PlayerData.builder()
                .id(id).teamId(TEAM).name(id).position(position)
                .matchesPlayed(apps).goals(goals).assists(assists)
                .passes(passes).duelsWon(duelsWon).shotsTotal(shots)
                .build();
    }

    /** A squad with more than ten eligible players, six of them forwards. */
    private static List<PlayerData> deepSquad() {
        List<PlayerData> squad = new ArrayList<>();
        squad.add(player("gk-first", "Goalkeepers", 9, 0, 0, 200, 5, 0));
        squad.add(player("gk-backup", "Goalkeepers", 1, 0, 0, 20, 0, 0));
        for (int i = 1; i <= 6; i++) {
            squad.add(player("fw-" + i, "Forwards", 9 - (i % 3), 6 - i / 2, 2, 120, 15, 25));
        }
        for (int i = 1; i <= 5; i++) {
            squad.add(player("mid-" + i, "Midfielders", 9, 1, 3, 700, 30, 8));
        }
        for (int i = 1; i <= 5; i++) {
            squad.add(player("def-" + i, "Defenders", 9, 0, 1, 500, 25, 3));
        }
        // Never played — must be dropped.
        squad.add(player("unused-fw", "Forwards", null, 0, 0, 0, 0, 0));
        squad.add(player("unused-def", "Defenders", null, 0, 0, 0, 0, 0));
        return squad;
    }

    @Test
    void returnsExactlyTenNotablePlayers() {
        when(repository.findByTeamId(TEAM)).thenReturn(deepSquad());

        assertThat(service().getNotablePlayers(TEAM)).hasSize(10);
    }

    @Test
    void alwaysIncludesTheFirstChoiceGoalkeeper() {
        when(repository.findByTeamId(TEAM)).thenReturn(deepSquad());

        assertThat(service().getNotablePlayers(TEAM))
                .extracting(PlayerData::getId)
                .contains("gk-first")
                .doesNotContain("gk-backup");
    }

    @Test
    void reservesTheKeeperSlotEvenWhenNoKeeperHasPlayed() {
        List<PlayerData> squad = deepSquad();
        squad.removeIf(player -> player.getId().startsWith("gk-"));
        squad.add(player("gk-a", "Goalkeepers", null, 0, 0, 0, 0, 0));
        squad.add(player("gk-b", "Goalkeepers", null, 0, 0, 0, 0, 0));
        when(repository.findByTeamId(TEAM)).thenReturn(squad);

        assertThat(service().getNotablePlayers(TEAM))
                .extracting(PlayerData::getPosition)
                .contains("Goalkeepers");
    }

    @Test
    void dropsOutfieldPlayersWithNoAppearances() {
        when(repository.findByTeamId(TEAM)).thenReturn(deepSquad());

        assertThat(service().getNotablePlayers(TEAM))
                .extracting(PlayerData::getId)
                .doesNotContain("unused-fw", "unused-def");
    }

    @Test
    void capsAnyOnePositionBucketAtFour() {
        when(repository.findByTeamId(TEAM)).thenReturn(deepSquad());

        List<PlayerData> notable = service().getNotablePlayers(TEAM);

        assertThat(notable).filteredOn(player -> "Forwards".equals(player.getPosition())).hasSizeLessThanOrEqualTo(4);
        assertThat(notable).filteredOn(player -> "Midfielders".equals(player.getPosition())).hasSizeLessThanOrEqualTo(4);
    }

    @Test
    void aHighVolumeMidfielderOutranksAFringeForward() {
        List<PlayerData> squad = new ArrayList<>();
        squad.add(player("gk", "Goalkeepers", 9, 0, 0, 150, 3, 0));
        PlayerData volumeMidfielder = player("engine", "Midfielders", 10, 1, 4, 1100, 60, 6);
        PlayerData fringeForward = player("fringe", "Forwards", 2, 1, 0, 30, 3, 4);
        squad.add(volumeMidfielder);
        squad.add(fringeForward);
        // Padding so the fringe forward is not kept only because there is room to spare.
        for (int i = 1; i <= 6; i++) {
            squad.add(player("fw-" + i, "Forwards", 8, 5, 1, 140, 18, 22));
        }
        when(repository.findByTeamId(TEAM)).thenReturn(squad);

        List<PlayerData> notable = service().getNotablePlayers(TEAM);

        assertThat(notable).contains(volumeMidfielder);
        assertThat(notable).doesNotContain(fringeForward);
    }

    @Test
    void returnsAnEmptyListForATeamWithNoStoredSquad() {
        when(repository.findByTeamId(TEAM)).thenReturn(List.of());

        assertThat(service().getNotablePlayers(TEAM)).isEmpty();
    }
}
