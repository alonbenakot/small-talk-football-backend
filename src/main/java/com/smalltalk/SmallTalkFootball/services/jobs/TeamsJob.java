package com.smalltalk.SmallTalkFootball.services.jobs;

import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Twice-weekly squad refresh. Squad stats — goals, ratings and above all {@code player_injured} —
 * change every matchday, and the team one-liner names an injured regular ahead of every other
 * fact, so stale player data does not merely age: it produces confidently wrong sentences.
 * <p>
 * Monday and Thursday because that is where the rounds fall: Monday picks up the weekend, Thursday
 * the midweek fixtures. It costs 14 apifootball calls a run (a {@code get_teams} and a
 * {@code get_topscorers} per competition), against roughly 56 a day that the fixture and standings
 * jobs already spend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamsJob {

    private final TeamDataService teamDataService;

    @Scheduled(cron = "0 0 4 * * MON,THU", zone = "Asia/Jerusalem")
    public void runJob() {
        teamDataService.saveCompetitionTeams();
        log.info("TeamsJob completed");
    }
}
