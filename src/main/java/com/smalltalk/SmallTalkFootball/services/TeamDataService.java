package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.repositories.TeamDataRepository;
import com.smalltalk.SmallTalkFootball.system.utils.TeamDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamDataService {
    private final TeamDataRepository repository;

    private final FootballApiService service;

    public void saveCompetitionTeams(Competition competition) {
        repository.deleteByCompetitionCode(competition.getCode());
        List<TeamData> teams = service.getTeamData(competition)
                .stream()
                .map(team -> TeamDataMapper.map(team, competition))
                .toList();

        repository.saveAll(teams);
    }

    public List<TeamData> getTeamDataByCompetition(Competition competition) {
        return repository.findByCompetitionCode(competition.getCode());
    }

    public Fixture enrichTeamsData(Fixture fixture, List<TeamData> teamDataList) {
        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();

        if (isMissingData(homeTeam)) {
            fillMissingData(homeTeam, teamDataList);
        }
        if (isMissingData(awayTeam)) {
            fillMissingData(awayTeam, teamDataList);
        }

        return fixture;
    }

    private void fillMissingData(Team team, List<TeamData> teamDataList) {
        teamDataList.stream()
                .filter(teamData -> team.getExternalId().equals(teamData.getExternalKey()))
                .findAny()
                .ifPresent(teamData -> applyTeamData(team, teamData));
    }

    private static boolean isMissingData(Team team) {
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
