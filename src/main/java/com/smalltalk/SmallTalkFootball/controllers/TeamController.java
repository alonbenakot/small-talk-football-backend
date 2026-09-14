package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.models.TeamSummary;
import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("teams")
public class TeamController {

    private final TeamDataService service;

    /** Public: JwtAuthFilter gates /teams for every method but GET. */
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<List<TeamSummary>> getTeams(@RequestParam Competition competition) {
        return new SmallTalkResponse<>(service.getTeamsByCompetition(competition));
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public void saveTeamsData() {
        service.saveCompetitionTeams();
    }

    @PatchMapping("/standings")
    @ResponseStatus(HttpStatus.OK)
    public void refreshStandings() {
        service.refreshStandings();
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeams() {
        service.deleteTeams();
    }
}
