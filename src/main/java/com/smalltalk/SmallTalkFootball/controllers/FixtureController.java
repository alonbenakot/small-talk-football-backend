package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.services.FixtureService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fixtures")
@AllArgsConstructor
public class FixtureController {

    private final FixtureService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<List<Fixture>> fetchAndSaveFixtures(@RequestParam int matchDays) {
        return new SmallTalkResponse<>(service.fetchAndSaveFixtures(matchDays));
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<FixturesResponse> getFixtures() {
        return new SmallTalkResponse<>(service.getFixtures());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SmallTalkResponse<Fixture> getOneFixture(@PathVariable String id) throws SmallTalkException {
        return new SmallTalkResponse<>(service.getFixture(id));
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.OK)
    public void deleteAllFixture() {
        service.deleteAllFixtures();
    }
}
