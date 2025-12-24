package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.FixtureService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FixturesJob {

    private final FixtureService service;

    @Value("${max.match.days}")
    private int maxMatchDays;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Jerusalem")
    public void runJob() {
        service.fetchAndSaveFixtures(maxMatchDays);
    }

}
