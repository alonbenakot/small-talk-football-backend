package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.models.PlayerSummary;
import com.smalltalk.SmallTalkFootball.models.SquadContext;
import com.smalltalk.SmallTalkFootball.repositories.PlayerDataRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Reads the squad written by {@link TeamDataService#saveCompetitionTeams()} and ranks it
 * into the "notable players" list the team one-liner card shows.
 *
 * <p>The ranking (see the plan, §6.4):
 * <ul>
 *   <li>a squad member with no appearances is not notable and is dropped — apifootball
 *       sends a blank string, which the mapper stores as {@code null};</li>
 *   <li>one slot is reserved unconditionally for the first-choice goalkeeper (the
 *       {@code Goalkeepers} entry with the most appearances, or simply the first one when
 *       none has played) — "who's in goal" is basic knowledge a keeper would never earn
 *       on attacking stats;</li>
 *   <li>the rest are scored on appearances relative to the squad's busiest player, plus
 *       goal contribution, plus a volume term (passes, duels won, shots) so a defender or
 *       midfielder can place at all;</li>
 *   <li>at most {@value #PER_POSITION_CAP} from any one position bucket, so the list does
 *       not degenerate into nine forwards.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PlayerDataService {

    static final int NOTABLE_PLAYER_COUNT = 10;
    private static final int PER_POSITION_CAP = 4;
    private static final List<String> POSITION_ORDER = List.of("Goalkeepers", "Defenders", "Midfielders", "Forwards");

    private final PlayerDataRepository repository;

    public List<PlayerData> getPlayersByTeam(String teamId) {
        return repository.findByTeamId(teamId);
    }

    /** A full document replace, which is fine: the caller just loaded the whole document, one-liners included. */
    public PlayerData save(PlayerData player) {
        return repository.save(player);
    }

    /** A 404 rather than a 500: the id is user-supplied through the player one-liner route. */
    public PlayerData getPlayerById(String id) throws NotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(Messages.NO_PLAYER_FOUND.formatted(id)));
    }

    /** The picker list, ordered by position bucket then shirt number so it reads like a squad list. */
    public List<PlayerSummary> getSquadSummaries(String teamId) {
        return repository.findByTeamId(teamId).stream()
                .sorted(Comparator.comparingInt(PlayerDataService::positionOrder)
                        .thenComparingInt(PlayerDataService::shirtNumber))
                .map(PlayerSummary::from)
                .toList();
    }

    private static int positionOrder(PlayerData player) {
        int index = player.getPosition() == null ? -1 : POSITION_ORDER.indexOf(player.getPosition());
        return index < 0 ? POSITION_ORDER.size() : index;
    }

    /** Shirt numbers are strings in the feed and blank for some squad members; those sort last. */
    private static int shirtNumber(PlayerData player) {
        try {
            return Integer.parseInt(player.getNumber());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public List<PlayerData> getNotablePlayers(String teamId) {
        List<PlayerData> squad = repository.findByTeamId(teamId);
        if (squad.isEmpty()) {
            return List.of();
        }

        PlayerData keeper = SquadContext.firstChoiceKeeper(squad);

        int maxAppearances = squad.stream()
                .filter(PlayerDataService::hasPlayed)
                .mapToInt(PlayerData::getMatchesPlayed)
                .max()
                .orElse(1);

        List<PlayerData> ranked = squad.stream()
                .filter(PlayerDataService::hasPlayed)
                .filter(player -> player != keeper)
                .sorted(Comparator.comparingDouble((PlayerData player) -> score(player, maxAppearances)).reversed())
                .toList();

        List<PlayerData> notable = new ArrayList<>();
        Map<String, Integer> perPosition = new HashMap<>();

        if (keeper != null) {
            notable.add(keeper);
            perPosition.merge(bucket(keeper), 1, Integer::sum);
        }

        for (PlayerData player : ranked) {
            if (notable.size() >= NOTABLE_PLAYER_COUNT) {
                break;
            }
            String bucket = bucket(player);
            if (perPosition.getOrDefault(bucket, 0) >= PER_POSITION_CAP) {
                continue;
            }
            notable.add(player);
            perPosition.merge(bucket, 1, Integer::sum);
        }

        return notable;
    }

    private static boolean hasPlayed(PlayerData player) {
        return player.getMatchesPlayed() != null && player.getMatchesPlayed() > 0;
    }

    private static double score(PlayerData player, int maxAppearances) {
        double appearanceShare = maxAppearances == 0 ? 0 : (double) nz(player.getMatchesPlayed()) / maxAppearances;
        double goalContribution = 2.0 * nz(player.getGoals()) + nz(player.getAssists());
        double volume = nz(player.getPasses()) / 100.0
                + nz(player.getDuelsWon()) / 10.0
                + nz(player.getShotsTotal()) / 5.0;
        return appearanceShare * 3.0 + goalContribution + volume;
    }

    private static String bucket(PlayerData player) {
        return player.getPosition() == null ? "" : player.getPosition();
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
