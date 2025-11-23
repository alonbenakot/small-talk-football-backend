package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("teams")
public class TeamController {

    private final TeamDataService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public void saveTeamsData() {
        service.saveCompetitionTeams();
    }
}
