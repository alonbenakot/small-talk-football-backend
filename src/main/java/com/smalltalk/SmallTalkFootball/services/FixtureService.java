package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.SummaryMatchDto;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class FixtureService {

    private final FootballApiService footBallApiService;
    private final TeamDataService teamService;
    private final FixtureRepository repo;
    private final Mapper<MatchDto, Fixture> fullFixtureMapper;

    public FixtureService(FootballApiService footBallApiService,
                          TeamDataService teamService, FixtureRepository repo,
                          Mapper<MatchDto, Fixture> fullFixtureMapper,
                          @Qualifier("summaryFixtureMapper") Mapper<SummaryMatchDto, Fixture> summaryFixtureMapper) {
        this.footBallApiService = footBallApiService;
        this.teamService = teamService;
        this.repo = repo;
        this.fullFixtureMapper = fullFixtureMapper;
    }

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
        //TODO change finished false to true if needed
        return footBallApiService.getMatches(earliestMatchDay)
                .stream()
                .map(fullFixtureMapper::map)
                .filter(fixture -> !externalIds.contains(fixture.getExternalId()) || !fixture.isFinished())
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
