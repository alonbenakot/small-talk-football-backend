package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.GoalscorerItem;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;

import java.util.List;

public class FixtureMapper {

    public static Fixture map(MatchDto matchDto) {
        return Fixture.builder()
                .externalId(Integer.parseInt(matchDto.getMatchId()))
                .competition(Competition.fromCode(Integer.parseInt(matchDto.getLeagueId())))
                .venue(matchDto.getMatchStadium())
                .homeTeam(mapTeam(matchDto, TeamType.HOME))
                .awayTeam(mapTeam(matchDto, TeamType.AWAY))
                .utcDate(matchDto.getMatchDate())
                .matchStartTime(matchDto.getMatchTime())
                .score(mapScore(matchDto))
                .goals(mapGoals(matchDto))
                .build();
    }

    private static List<Goal> mapGoals(MatchDto matchDto) {
        List<GoalscorerItem> goalsDto = matchDto.getGoalscorer();
        if (goalsDto == null) return List.of();

        return goalsDto.stream()
                .map(goalDto -> mapSingleGoal(goalDto, matchDto))
                .toList();
    }

    private static Goal mapSingleGoal(GoalscorerItem goalDto, MatchDto matchDto) {
        TeamType teamType = determineTeamType(goalDto);
        String goalBy = getGoalBy(goalDto, teamType);
        String assistBy = getAssistBy(goalDto, teamType);
        String teamName = getTeamName(matchDto, teamType);

        String[] scoreParts = goalDto.getScore().split(" - ");
        int homeScore = Integer.parseInt(scoreParts[0].trim());
        int awayScore = Integer.parseInt(scoreParts[1].trim());

        return Goal.builder()
                .minute(mapMinute(goalDto))
                .goalBy(goalBy)
                .assistBy(assistBy)
                .teamName(teamName)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();
    }

    private static int mapMinute(GoalscorerItem goalDto) {
        String time = goalDto.getTime();
        if (time.contains("+")) {
            String[] splitTime = time.split("\\+");
            return Integer.parseInt(splitTime[0].trim()) + Integer.parseInt(splitTime[1].trim());
        }
        return Integer.parseInt(time);
    }

    private static TeamType determineTeamType(GoalscorerItem goalDto) {
        return goalDto.getHomeScorer() != null && !goalDto.getHomeScorer().isBlank()
                ? TeamType.HOME : TeamType.AWAY;
    }

    private static String getGoalBy(GoalscorerItem goalDto, TeamType teamType) {
        return teamType == TeamType.HOME ? goalDto.getHomeScorer() : goalDto.getAwayScorer();
    }

    private static String getAssistBy(GoalscorerItem goalDto, TeamType teamType) {
        return teamType == TeamType.HOME ? goalDto.getHomeAssist() : goalDto.getAwayAssist();
    }

    private static String getTeamName(MatchDto matchDto, TeamType teamType) {
        return teamType == TeamType.HOME ? matchDto.getMatchHomeTeamName() : matchDto.getMatchAwayTeamName();
    }

    private static Score mapScore(MatchDto matchDto) {
        int homeScore;
        int awayScore;
        try {
            homeScore = Integer.parseInt(matchDto.getMatchHometeamScore());
            awayScore = Integer.parseInt(matchDto.getMatchAwayteamScore());
        } catch (Exception e) {
            int[] homeAndAwayScore = deriveScoreFromGoals(matchDto);
            homeScore = homeAndAwayScore[0];
            awayScore = homeAndAwayScore[1];
        }
        boolean isDraw = homeScore == awayScore;

        String winner = isDraw ? null
                : (homeScore > awayScore ? matchDto.getMatchHomeTeamName() : matchDto.getMatchAwayTeamName());

        return Score.builder()
                .home(homeScore)
                .away(awayScore)
                .draw(isDraw)
                .winner(winner)
                .build();
    }

    private static int[] deriveScoreFromGoals(MatchDto matchDto) {
        if (matchDto.getGoalscorer() == null || matchDto.getGoalscorer().isEmpty()) {
            return new int[]{0, 0};
        }

        GoalscorerItem lastGoal = matchDto.getGoalscorer().get(matchDto.getGoalscorer().size() - 1);
        String[] scoreParts = lastGoal.getScore().split(" - ");
        int homeScore = Integer.parseInt(scoreParts[0].trim());
        int awayScore = Integer.parseInt(scoreParts[1].trim());

        return new int[]{homeScore, awayScore};
    }

    private static Team mapTeam(MatchDto matchDto, TeamType teamType) {
        String teamId = teamType == TeamType.HOME ? matchDto.getMatchHometeamId() : matchDto.getMatchAwayteamId();
        String crest = teamType == TeamType.HOME ? matchDto.getTeamHomeBadge() : matchDto.getTeamAwayBadge();
        String name = teamType == TeamType.HOME ? matchDto.getMatchHomeTeamName() : matchDto.getMatchAwayTeamName();
        String formation = teamType == TeamType.HOME ? matchDto.getMatchHometeamSystem() : matchDto.getMatchAwayteamSystem();
        String coach = getCoach(matchDto, teamType);

        return Team.builder()
                .id(teamId)
                .crest(crest)
                .name(name)
                .coach(coach)
                .formation(formation)
                .build();
    }

    private static String getCoach(MatchDto matchDto, TeamType teamType) {
        try {
            return teamType == TeamType.HOME
                    ? matchDto.getMatchLineup().getHomeLineUp().getCoaches().get(0).getLineupPlayer()
                    : matchDto.getMatchLineup().getAwayLineUp().getCoaches().get(0).getLineupPlayer();
        } catch (Exception e) {
            return null;
        }
    }

    enum TeamType {
        HOME, AWAY
    }

}
