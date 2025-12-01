package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.FixtureMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
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

    public FixturesResponse getFixtures() {
        List<Fixture> fixtures = repo.findAll();
        fixtures.sort(Comparator.comparing(Fixture::getCompetition));
        return new FixturesResponse(Arrays.asList(Competition.values()), fixtures);
    }

    public Fixture getFixture(String id) throws SmallTalkException {
        return repo.findById(id).orElseThrow(() -> new SmallTalkException("Invalid fixture id"));
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
        Instant startOfDay = deleteMatchesDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        long deletedFixtures = repo.deleteByMatchDateTimeBefore(startOfDay);
        log.info("Deleted {} fixtures", deletedFixtures);
    }

    public void deleteAllFixtures() {
        repo.deleteAll();
    }

    public void saveFixture(Fixture fixture) {
        repo.save(fixture);
    }

    private Set<Integer> getFixturesExternalIds(LocalDate earliestMatchDay) {
        Instant startOfDay = earliestMatchDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        return repo.findByMatchDateTimeAfter(startOfDay).stream()
                .map(Fixture::getExternalId)
                .collect(Collectors.toSet());
    }

}
