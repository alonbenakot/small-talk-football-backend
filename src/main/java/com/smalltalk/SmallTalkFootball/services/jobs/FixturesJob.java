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
    @Value("${match.days.into.future}")
    private int matchDaysIntoFuture;

    @Scheduled(cron = "0 0 7,20,21 * * *", zone = "Asia/Jerusalem")
    public void runJobDay() {
        runJob();
    }

    @Scheduled(cron = "0 30 23,0 * * *", zone = "Asia/Jerusalem")
    public void runJobNight() {
        runJob();
    }

    private void runJob() {
        service.fetchAndSaveFixtures(maxMatchDays, matchDaysIntoFuture);
        log.info("FixturesJob completed");
    }

}
