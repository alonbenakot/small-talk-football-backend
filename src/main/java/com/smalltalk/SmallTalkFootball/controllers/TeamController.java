package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.models.TeamFacts;
import com.smalltalk.SmallTalkFootball.models.TeamsResponse;
import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import com.smalltalk.SmallTalkFootball.services.TeamOneLinersService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("teams")
public class TeamController {

    private final TeamDataService service;

    private final TeamOneLinersService oneLinersService;

    /** Public: JwtAuthFilter gates /teams for every method but GET. */
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<TeamsResponse> getTeams() {
        return new SmallTalkResponse<>(service.getTeams());
    }

    /** Public: the team page's facts block, same shape as the one-liner's facts but with no sentence. */
    @GetMapping("/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<TeamFacts> getTeamFacts(@PathVariable String teamId) throws NotFoundException {
        return new SmallTalkResponse<>(oneLinersService.getTeamFacts(teamId));
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
