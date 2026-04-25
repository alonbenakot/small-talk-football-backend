package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.FixtureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixturesJob {

    private final FixtureService service;

    @Value("${max.match.days}")
    private int maxMatchDays;

    @Scheduled(cron = "0 40 0,19,21,22 * * *", zone = "Asia/Jerusalem")
    public void runJob() {
        service.fetchAndSaveFixtures(maxMatchDays);
        log.debug("FixturesJob completed");
    }

}
