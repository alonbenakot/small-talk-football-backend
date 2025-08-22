package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.utils.FixtureMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class FixtureService {

    private final FootballApiService footBallApiService;
    private final TeamService teamService;
    private final FixtureRepository repo;

    public List<Fixture> getLastWeekFixtures(Competition competition) {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        return footBallApiService.getMatches(sevenDaysAgo, competition)
                .stream()
                .map(FixtureMapper::map)
                .map(teamService::enrichTeamsData)
                .toList();
    }

    public void deleteAllFixtures() {
        repo.deleteAll();
    }

}
