package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.TeamDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamDataService {
    private final TeamDataRepository repository;

    private final FootballApiService service;

    public void saveCompetitionTeams() {

        List<TeamData> teams = Arrays.stream(Competition.values())
                .flatMap(competition -> service.getTeamData(competition).stream()
                        .map(team -> TeamDataMapper.map(team, competition)))
                .toList();

        if (!teams.isEmpty()) {
            repository.deleteAll();
            repository.saveAll(teams);
        }
    }

    public List<TeamData> getTeamsData() {
        return repository.findAll();
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
                .filter(teamData -> team.getExternalId().equals(teamData.getExternalKey()))
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

}
