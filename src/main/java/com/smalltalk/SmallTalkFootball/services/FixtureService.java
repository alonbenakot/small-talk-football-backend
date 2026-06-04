package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.repositories.FixtureRepository;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
                          Mapper<MatchDto, Fixture> fullFixtureMapper) {
        this.footBallApiService = footBallApiService;
        this.teamService = teamService;
        this.repo = repo;
        this.fullFixtureMapper = fullFixtureMapper;
    }

    public List<Fixture> fetchAndSaveFixtures(int matchDays, int matchDaysIntoFuture) {

        LocalDate earliestMatchDay = LocalDate.now().minusDays(matchDays);
        LocalDate latestMatchDay = LocalDate.now().plusDays(matchDaysIntoFuture);

        List<Fixture> fixtures = fetchNewFixtures(earliestMatchDay, latestMatchDay);

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
        List<Competition> competitionsWithFixtures = fixtures.stream()
                .map(Fixture::getCompetition)
                .distinct()
                .toList();
        return new FixturesResponse(competitionsWithFixtures, fixtures);
    }

    public Fixture getFixture(String id) throws SmallTalkException {
        return repo.findById(id).orElseThrow(() -> new SmallTalkException("Invalid fixture id"));
    }

    private List<Fixture> fetchNewFixtures(LocalDate earliestMatchDay, LocalDate latestMatchDay) {
        List<TeamData> allTeamsData = teamService.getTeamsData();
        Map<Integer, Fixture> existingFixtures = getExistingFixturesByExternalId(earliestMatchDay);

        return footBallApiService.getMatches(earliestMatchDay, latestMatchDay)
                .stream()
                .map(fullFixtureMapper::map)
                .filter(fixture -> isNewOrNotFinished(fixture, existingFixtures))
                .map(fixture -> preserveExistingId(fixture, existingFixtures))
                .map(fixture -> teamService.enrichTeamsData(fixture, allTeamsData))
                .toList();
    }

    private boolean isNewOrNotFinished(Fixture fixture, Map<Integer, Fixture> existingFixtures) {
        Fixture existing = existingFixtures.get(fixture.getExternalId());
        return existing == null || !existing.isFinished();
    }

    private Fixture preserveExistingId(Fixture fixture, Map<Integer, Fixture> existingFixtures) {
        Fixture existing = existingFixtures.get(fixture.getExternalId());
        if (existing != null) {
            fixture.setId(existing.getId());
        }
        return fixture;
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

    private Map<Integer, Fixture> getExistingFixturesByExternalId(LocalDate earliestMatchDay) {
        Instant startOfDay = earliestMatchDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        return repo.findByMatchDateTimeAfter(startOfDay).stream()
                .collect(Collectors.toMap(Fixture::getExternalId, Function.identity(), (a, b) -> b));
    }

}
