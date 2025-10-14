package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.services.OneLinersService;
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

    @GetMapping("/{fixtureId}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<OneLiner> getOneLiner(@PathVariable String fixtureId,
                                                   @RequestParam(required = false) TeamType teamType,
                                                   @RequestParam Language lang) throws SmallTalkException {
        return new SmallTalkResponse<>(service.getOneLiner(fixtureId, teamType, lang));
    }
}
