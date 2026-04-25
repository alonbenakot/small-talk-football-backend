package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StandingsJob {

    private final TeamDataService teamDataService;

    @Scheduled(cron = "0 30 0,19,21 * * *", zone = "Asia/Jerusalem")
    public void runJob() {
        teamDataService.refreshStandings();
    }
}
