package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.models.PlayerSmallTalk;
import com.smalltalk.SmallTalkFootball.models.TeamSmallTalk;
import com.smalltalk.SmallTalkFootball.services.OneLinersService;
import com.smalltalk.SmallTalkFootball.services.PlayerOneLinersService;
import com.smalltalk.SmallTalkFootball.services.TeamOneLinersService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("one-liners")
@RequiredArgsConstructor
public class OneLinerController {

    private final OneLinersService service;

    private final TeamOneLinersService teamService;

    private final PlayerOneLinersService playerService;

    @GetMapping("/{fixtureId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<OneLiner> getOneLiner(@PathVariable String fixtureId,
                                                   @RequestParam(required = false) TeamType teamType,
                                                   @RequestParam Language lang) throws SmallTalkException {
        return new SmallTalkResponse<>(service.getOneLiner(fixtureId, teamType, lang));
    }

    /**
     * Two path segments, so there is no collision with the fixture route above. It also keeps
     * the team small talk off {@code /teams}, which JwtAuthFilter gates as admin-only in its
     * entirety — so this route needs no security change to stay public.
     */
    @GetMapping("/teams/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<TeamSmallTalk> getTeamOneLiner(@PathVariable String teamId,
                                                            @RequestParam Language lang,
                                                            @RequestParam(defaultValue = "NEUTRAL") Perspective perspective,
                                                            @RequestParam(required = false) Competition competition)
            throws SmallTalkException {
        return new SmallTalkResponse<>(teamService.getTeamSmallTalk(teamId, competition, lang, perspective));
    }

    /** Same reasoning as the team route: two segments, under {@code /one-liners}, so it stays public. */
    @GetMapping("/players/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<PlayerSmallTalk> getPlayerOneLiner(@PathVariable String playerId,
                                                                @RequestParam Language lang)
            throws SmallTalkException {
        return new SmallTalkResponse<>(playerService.getPlayerSmallTalk(playerId, lang));
    }
}
