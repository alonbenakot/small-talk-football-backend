package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.services.CompetitionDataService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("competitions")
@RequiredArgsConstructor
public class CompetitionDataController {

    private final CompetitionDataService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<List<CompetitionData>> fetchAndSaveCompetitions() {
        return new SmallTalkResponse<>(service.fetchAndSaveCompetitions());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<List<CompetitionData>> getCompetitions() {
        return new SmallTalkResponse<>(service.getCompetitions());
    }
}
