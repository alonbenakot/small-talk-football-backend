package com.smalltalk.SmallTalkFootball.controllers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.models.FixturesResponse;
import com.smalltalk.SmallTalkFootball.services.FixtureService;
import com.smalltalk.SmallTalkFootball.system.SmallTalkResponse;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("fixtures")
@AllArgsConstructor
@Validated
public class FixtureController {

    private final FixtureService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public SmallTalkResponse<List<Fixture>> fetchAndSaveFixtures(
            @RequestParam @Min(0) @Max(365) int matchDays,
            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int matchDaysIntoFuture) {
        return new SmallTalkResponse<>(service.fetchAndSaveFixtures(matchDays, matchDaysIntoFuture));
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
