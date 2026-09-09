package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.PlayerData;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.StandingsDtoItem;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamDataService {
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

                savePlayers(teamDto, scorerRankByPlayerId);
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

    private void savePlayers(TeamDataDto teamDto, Map<String, Integer> scorerRankByPlayerId) {
        playerDataMapper.map(teamDto).forEach(player -> {
            player.setLeagueScorerRank(scorerRankByPlayerId.get(player.getId()));
            mongoTemplate.save(player);
        });
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

    public TeamData getTeamById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("TeamData not found for id: " + id));
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
