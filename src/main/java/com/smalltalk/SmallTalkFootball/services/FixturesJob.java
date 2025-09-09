package com.smalltalk.SmallTalkFootball.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FixturesJob {

    private final FixtureService service;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Jerusalem")
    public void runJob() {
        service.fetchAndSaveFixtures(8);
    }

}
