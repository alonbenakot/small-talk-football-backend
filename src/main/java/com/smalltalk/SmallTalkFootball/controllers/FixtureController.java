package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
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

    @PostMapping()
    public SmallTalkResponse<List<Fixture>> fetchAndSaveFixtures(@RequestParam int matchDays) {
        return new SmallTalkResponse<>(service.fetchAndSaveFixtures(matchDays));
    }

    @GetMapping()
    public SmallTalkResponse<List<Fixture>> getFixtures() {
        return new SmallTalkResponse<>(service.getFixtures());
    }

    @DeleteMapping()
    public void deleteAllFixture() {
        service.deleteAllFixtures();
    }
}
