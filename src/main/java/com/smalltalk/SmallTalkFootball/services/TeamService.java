package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.repositories.TeamRepository;
import com.smalltalk.SmallTalkFootball.system.utils.TeamDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository repository;
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
        String homeId = homeTeam.getId();
        String awayId = awayTeam.getId();

        if (homeTeam.getName() == null || homeTeam.getCoach() == null || homeTeam.getFormation() == null) {
            repository.findByExternalKey(homeId);
        }

        return fixture;
    }
}
