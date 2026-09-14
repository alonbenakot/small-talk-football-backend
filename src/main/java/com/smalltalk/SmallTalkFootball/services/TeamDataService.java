package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.TeamSummary;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import com.smalltalk.SmallTalkFootball.system.messages.Messages;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TeamDataService {

    /**
     * Fields on {@link PlayerData} that the squad refresh never writes, so a {@code $set}
     * upsert leaves them exactly as they are. {@code _id} is set by the upsert query;
     * {@code oneLiners} is the player one-liner cache, which only its own service writes.
     */
    private static final Set<String> FIELDS_NOT_REFRESHED = Set.of("_id", "oneLiners");

    private final TeamDataRepository repository;

    private final MongoTemplate mongoTemplate;

    private final FootballApiService service;

    private final Mapper<StandingsDtoItem, Standing> standingMapper;

    private final Mapper<TeamDataDto, Update> teamDataUpdateMapper;

    private final Mapper<TeamDataDto, List<PlayerData>> playerDataMapper;

    public TeamDataService(
            TeamDataRepository repository,
            MongoTemplate mongoTemplate,
            FootballApiService service,
            @Qualifier("standingMapper") Mapper<StandingsDtoItem, Standing> competitionRatingMapper,
            @Qualifier("teamDataUpdateMapper") Mapper<TeamDataDto, Update> teamDataUpdateMapper,
            @Qualifier("playerDataMapper") Mapper<TeamDataDto, List<PlayerData>> playerDataMapper) {
        this.repository = repository;
        this.service = service;
        this.standingMapper = competitionRatingMapper;
        this.teamDataUpdateMapper = teamDataUpdateMapper;
        this.playerDataMapper = playerDataMapper;
        this.mongoTemplate = mongoTemplate;
    }

    public void saveCompetitionTeams() {

        Arrays.stream(Competition.values()).forEach(competition -> {

            Map<String, Integer> scorerRankByPlayerId = leagueScorerRanks(competition);

            service.getTeamDataList(competition).forEach(teamDto -> {

                Query query = Query.query(Criteria.where("_id").is(teamDto.getTeamKey()));

                Update update = teamDataUpdateMapper.map(teamDto)
                        .setOnInsert("_id", teamDto.getTeamKey())
                        .setOnInsert("standings", new EnumMap<>(Competition.class));

                mongoTemplate.upsert(query, update, TeamData.class);

                savePlayers(teamDto, scorerRankByPlayerId, competition);
            });

        });
    }

    public TeamData save(TeamData team) {
        return repository.save(team);
    }

    /**
     * A player_id → league-scoring-charts place map for one competition, from
     * {@code get_topscorers}. {@code player_key} in that response is the same identifier as
     * {@code player_id} in {@code get_teams}, so it keys straight onto the stored player.
     */
    private Map<String, Integer> leagueScorerRanks(Competition competition) {
        Map<String, Integer> ranks = new HashMap<>();
        service.getTopScorers(competition).forEach(scorer -> {
            Integer place = parsePlace(scorer.getPlayerPlace());
            if (scorer.getPlayerKey() != null && place != null) {
                ranks.putIfAbsent(scorer.getPlayerKey(), place);
            }
        });
        return ranks;
    }

    /**
     * One bulk write per team rather than one round-trip per player. A full refresh covers some
     * 5,500 players, and saving them one at a time made the run take eleven minutes against a
     * hosted database — almost all of it latency, since the whole job makes only fourteen calls
     * to apifootball.
     * <p>
     * Each player is an upsert with {@code $set} of the mapped fields rather than a replace, so
     * anything the mapper does not produce ({@link #FIELDS_NOT_REFRESHED}) survives the refresh.
     * The converter omits null values, so a field that went from a value to null (a scorer rank
     * that dropped out of the charts) is explicitly {@code $unset} — otherwise {@code $set}
     * would leave last week's value in place.
     * <p>
     * The same bulk removes every stored player of this team who is not in the payload — a
     * player who left the tracked leagues would otherwise keep his {@code teamId} forever
     * (bug #11). The removal only runs from the domestic-league call, so two competitions
     * refreshing the same club cannot delete each other's players, and it is skipped when the
     * payload is smaller than half the stored squad, since that is what a truncated response
     * looks like. The empty check matters twice over: national teams come back with no squad
     * at all, {@code execute()} rejects a bulk holding no operations, and an empty payload must
     * never be read as "everyone left".
     */
    private void savePlayers(TeamDataDto teamDto, Map<String, Integer> scorerRankByPlayerId, Competition competition) {
        List<PlayerData> players = playerDataMapper.map(teamDto);
        if (players.isEmpty()) {
            return;
        }

        MongoPersistentEntity<?> entity = mongoTemplate.getConverter().getMappingContext()
                .getRequiredPersistentEntity(PlayerData.class);

        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PlayerData.class);
        players.forEach(player -> {
            player.setLeagueScorerRank(scorerRankByPlayerId.get(player.getId()));

            Document mapped = new Document();
            mongoTemplate.getConverter().write(player, mapped);
            Update update = new Update();
            mapped.forEach((field, value) -> {
                if (!FIELDS_NOT_REFRESHED.contains(field)) {
                    update.set(field, value);
                }
            });
            entity.forEach(property -> {
                String field = property.getFieldName();
                if (!mapped.containsKey(field) && !FIELDS_NOT_REFRESHED.contains(field)) {
                    update.unset(field);
                }
            });

            bulk.upsert(Query.query(Criteria.where("_id").is(player.getId())), update);
        });

        if (isDomesticLeague(competition)) {
            removeDepartedPlayers(bulk, teamDto, players);
        }
        bulk.execute();
    }

    private void removeDepartedPlayers(BulkOperations bulk, TeamDataDto teamDto, List<PlayerData> players) {
        String teamId = teamDto.getTeamKey();
        long stored = mongoTemplate.count(Query.query(Criteria.where("teamId").is(teamId)), PlayerData.class);
        if (players.size() * 2L < stored) {
            log.warn("Squad for team {} came back with {} players against {} stored; keeping the stored ones",
                    teamId, players.size(), stored);
            return;
        }
        List<String> writtenIds = players.stream().map(PlayerData::getId).toList();
        bulk.remove(Query.query(Criteria.where("teamId").is(teamId).and("_id").nin(writtenIds)));
    }

    private static boolean isDomesticLeague(Competition competition) {
        return competition != Competition.CHAMPIONS_LEAGUE && competition != Competition.WORLD_CUP;
    }

    private static Integer parsePlace(String place) {
        if (place == null || place.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(place.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void refreshStandings() {
        Map<String, TeamData> teamsById = repository.findAll().stream()
                .collect(Collectors.toMap(
                        TeamData::getId,
                        Function.identity()
                ));

        Arrays.stream(Competition.values()).forEach(competition -> {
            service.getCompetitionStandings(competition).forEach(standingsDto -> {

                TeamData team = teamsById.get(standingsDto.getTeamId());
                if (team == null) return;

                Standing standing = standingMapper.map(standingsDto);

                team.getStandings().put(
                        standing.getCompetition(),
                        standing
                );
            });
        });

        repository.saveAll(teamsById.values());
    }

    public List<TeamData> getTeamsData() {
        return repository.findAll();
    }

    /** The competition's teams in table order, for the Teams page. */
    public List<TeamSummary> getTeamsByCompetition(Competition competition) {
        return repository.findByCompetition(competition).stream()
                .map(team -> TeamSummary.from(team, competition))
                .sorted(Comparator.comparingInt(TeamSummary::position))
                .toList();
    }

    /**
     * Throws a {@link NotFoundException} (404) rather than an IllegalStateException: the team
     * id became user-supplied with the team one-liner route, so an unknown one has to reach
     * the ControllerAdvice as a 404 instead of a bare 500.
     */
    public TeamData getTeamById(String id) throws NotFoundException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(Messages.NO_TEAM_FOUND.formatted(id)));
    }

    /**
     * The null-tolerant twin of {@link #getTeamById}: a player whose club no longer resolves
     * should still get a sentence about himself, not a 404 about his team (plan §8).
     */
    public Optional<TeamData> findTeamById(String id) {
        return repository.findById(id);
    }

    public Fixture enrichTeamsData(Fixture fixture, List<TeamData> teamDataList) {
        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();

        if (isMissingTeamData(homeTeam)) {
            fillMissingData(homeTeam, teamDataList);
        }
        if (isMissingTeamData(awayTeam)) {
            fillMissingData(awayTeam, teamDataList);
        }

        if (isMissingTeamInGoals(fixture)) {
            fillMissingTeamInGoals(fixture);
        }

        if (fixture.getScore().getWinner() == null || fixture.getScore().getWinner().isBlank()) {
            deriveWinner(fixture);
        }

        return fixture;
    }

    private void deriveWinner(Fixture fixture) {
        Score score = fixture.getScore();
        score.setWinner(score.isDraw() ? null :
                (score.getHome() > score.getAway()
                        ? fixture.getHomeTeam().getName()
                        : fixture.getAwayTeam().getName()));
    }


    private void fillMissingTeamInGoals(Fixture fixture) {
        String homeTeamName = fixture.getHomeTeam().getName();
        String awayTeamName = fixture.getAwayTeam().getName();

        fixture.getGoals().forEach(goal -> goal.setTeamName(goal.getTeamType() == TeamType.HOME ? homeTeamName : awayTeamName));
    }

    private boolean isMissingTeamInGoals(Fixture fixture) {
        return fixture.getGoals().stream()
                .map(Goal::getTeamName)
                .anyMatch(teamName -> teamName == null || teamName.isBlank());
    }

    private void fillMissingData(Team team, List<TeamData> teamDataList) {
        teamDataList.stream()
                .filter(teamData -> team.getId().equals(teamData.getId()))
                .findAny()
                .ifPresent(teamData -> applyTeamData(team, teamData));
    }

    private static boolean isMissingTeamData(Team team) {
        return team.getName() == null || team.getCoach() == null || team.getCrest() == null;
    }

    private void applyTeamData(Team team, TeamData data) {
        if (team.getName() == null) {
            team.setName(data.getName());
        }
        if (team.getCoach() == null) {
            team.setCoach(data.getCoach());
        }
        if (team.getCrest() == null) {
            team.setCrest(data.getCrest());
        }
    }

    public void deleteTeams() {
        repository.deleteAll();
    }
}
