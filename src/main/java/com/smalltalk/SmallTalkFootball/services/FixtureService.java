package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.FixtureMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class FixtureService {

    private final FootballApiService footBallApiService;
    private final TeamDataService teamService;
    private final FixtureRepository repo;

    public List<Fixture> fetchAndSaveFixtures(int matchDays) {
        LocalDate earliestMatchDay = LocalDate.now().minusDays(matchDays);
        List<Fixture> fixtures = fetchNewFixtures(earliestMatchDay);

        if (!fixtures.isEmpty()) {
            deleteOldFixtures(earliestMatchDay);
            return repo.saveAll(fixtures);
        } else {
            log.info("No new fixtures found");
            return List.of();
        }

    }

    public List<Fixture> getFixtures() {
        return repo.findAll();
    }

    private List<Fixture> fetchNewFixtures(LocalDate earliestMatchDay) {
        List<TeamData> allTeamsData = teamService.getTeamsData();
        Set<Integer> externalIds = getFixturesExternalIds(earliestMatchDay);

        return footBallApiService.getMatches(earliestMatchDay)
                .stream()
                .map(FixtureMapper::map)
                .filter(Fixture::isFinished)
                .filter(fixture -> !externalIds.contains(fixture.getExternalId()))
                .map(fixture -> teamService.enrichTeamsData(fixture, allTeamsData))
                .toList();
    }

    private void deleteOldFixtures(LocalDate deleteMatchesDate) {
        long deletedFixtures = repo.deleteByMatchDateTimeBefore(deleteMatchesDate.atStartOfDay());
        log.info("Deleted {} fixtures", deletedFixtures);
    }

    public void deleteAllFixtures() {
        repo.deleteAll();
    }

    private Set<Integer> getFixturesExternalIds(LocalDate earliestMatchDay) {
        return repo.findByMatchDateTimeAfter(earliestMatchDay.atStartOfDay()).stream()
                .map(Fixture::getExternalId)
                .collect(Collectors.toSet());
    }

}
