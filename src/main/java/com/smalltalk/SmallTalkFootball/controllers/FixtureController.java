package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.services.FixtureService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fixtures")
@AllArgsConstructor
public class FixtureController {

    private final FixtureService service;


    @GetMapping()
    public SmallTalkResponse<List<Fixture>> getLastWeeksFixturesByCompetition(@RequestParam Competition competition) {
        return new SmallTalkResponse<>(service.getLastWeekFixtures(competition));
    }

    @DeleteMapping()
    public void deleteAllFixture() {
        service.deleteAllFixtures();
    }
}
