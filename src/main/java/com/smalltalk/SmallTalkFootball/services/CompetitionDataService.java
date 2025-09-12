package com.smalltalk.SmallTalkFootball.services;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.repositories.CompetitionDataRepository;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.CompetitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionDataService {

    private final CompetitionDataRepository repository;

    private final FootballApiService apiService;

    public List<CompetitionData> fetchAndSaveCompetitions() {
        List<CompetitionData> competitions = apiService.getCompetitionData().stream()
                .filter(competitionDto -> Competition.isValidCode(Integer.parseInt(competitionDto.getLeagueId())))
                .map(CompetitionMapper::map)
                .toList();

        if (!competitions.isEmpty()) {
            repository.deleteAll();
            repository.saveAll(competitions);
        }

        return competitions;
    }

    public List<CompetitionData> getCompetitions() {
        return repository.findAll();
    }

}

