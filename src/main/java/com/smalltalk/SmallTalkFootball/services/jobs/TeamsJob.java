package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily squad refresh. Squad stats (goals, injuries, ratings) change weekly, so without this the
 * notable-player data behind the team one-liners goes stale. One {@code get_teams} call per
 * competition per day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamsJob {

    private final TeamDataService teamDataService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Jerusalem")
    public void runJob() {
        teamDataService.saveCompetitionTeams();
        log.info("TeamsJob completed");
    }
}
