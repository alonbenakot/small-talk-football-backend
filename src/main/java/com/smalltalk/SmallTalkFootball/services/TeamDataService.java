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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamDataRepository repository;
    private final FootballApiService service;

    public void saveCompetitionTeams(Competition competition) {
        List<TeamData> teams = service.getTeamData(competition)
                .stream()
                .map(TeamDataMapper::map)
                .toList();

        repository.saveAll(teams);
    }

    public Fixture enrichTeamsData(Fixture fixture) {
        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();

        if (isMissingData(homeTeam)) {
            fillMissingData(homeTeam);
        }
        if (isMissingData(awayTeam)) {
            fillMissingData(awayTeam);
        }

        return fixture;
    }

    private void fillMissingData(Team team) {
        Optional<TeamData> teamDataOptional = repository.findByExternalKey(team.getExternalId());
        if (teamDataOptional.isPresent()) {
            TeamData teamData = teamDataOptional.get();
            if (team.getName() == null) {
                team.setName(teamData.getName());
            }
            if (team.getCoach() == null) {
                team.setCoach(teamData.getCoach());
            }
            if (team.getCrest() == null) {
                team.setCrest(teamData.getCrest());
            }
        }
    }

    private static boolean isMissingData(Team team) {
        return team.getName() == null || team.getCoach() == null || team.getCrest() == null;
    }
}
