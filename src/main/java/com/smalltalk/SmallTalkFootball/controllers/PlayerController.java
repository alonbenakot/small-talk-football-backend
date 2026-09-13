package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.models.PlayerSummary;
import com.smalltalk.SmallTalkFootball.services.PlayerDataService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The only way a player is discovered: the app shows a team's squad and the user picks from
 * it. {@code /players} matches no {@code isJwtRequired*} branch of JwtAuthFilter, so the route
 * is public with no filter change.
 */
@RestController
@RequestMapping("players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerDataService service;

    @GetMapping("/teams/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<List<PlayerSummary>> getSquad(@PathVariable String teamId) {
        return new SmallTalkResponse<>(service.getSquadSummaries(teamId));
    }
}
