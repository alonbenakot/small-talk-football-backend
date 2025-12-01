package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.StatType;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Statistic;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.GoalscorerItem;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import com.smalltalk.SmallTalkFootball.models.dto.StatisticDto;

import java.time.*;
import java.util.List;
import java.util.Objects;

public class FixtureMapper {

    public static final String FINISHED = "Finished";

    public static Fixture map(MatchDto matchDto) {
        return Fixture.builder()
                .externalId(Integer.parseInt(matchDto.getMatchId()))
                .competition(Competition.fromCode(Integer.parseInt(matchDto.getLeagueId())))
                .venue(matchDto.getMatchStadium())
                .homeTeam(mapTeam(matchDto, TeamType.HOME))
                .awayTeam(mapTeam(matchDto, TeamType.AWAY))
                .matchDateTime(mapTime(matchDto))
                .finished(FINISHED.equals(matchDto.getMatchStatus()))
                .score(mapScore(matchDto))
                .goals(mapGoals(matchDto))
                .statistics(mapStatistics(matchDto.getStatistics()))
                .build();
    }

    private static List<Statistic> mapStatistics(List<StatisticDto> statistics) {
        if (statistics == null) return List.of();

        return statistics.stream()
                .map(FixtureMapper::mapSingleStatistic)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Statistic mapSingleStatistic(StatisticDto stat) {
        if (!StatType.getStatTypesValue().contains(stat.getType())) {
            return null;
        }

        Integer home = parseStatistic(stat.getHome());
        Integer away = parseStatistic(stat.getAway());

        if (home == null || away == null) {
            return null;
        }

        return Statistic.builder()
                .type(stat.getType())
                .home(home)
                .away(away)
                .percentage(stat.getHome().contains("%"))
                .build();
    }

    private static Integer parseStatistic(String teamStat) {
        if (teamStat == null) return null;

        String cleaned = teamStat.replace("%", "")
                .replace("[", "")
                .replace("]", "")
                .trim();

        if (cleaned.isEmpty()) return null;

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant mapTime(MatchDto matchDto) {
        LocalDate date = LocalDate.parse(matchDto.getMatchDate());
        LocalTime time = LocalTime.parse(matchDto.getMatchTime());

        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        return localDateTime.toInstant(ZoneOffset.UTC);
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

        String rawScore = goalDto.getScore().replace("[", "").replace("]", "");
        String[] scoreParts = rawScore.split(" - ");
        int homeScore = Integer.parseInt(scoreParts[0].trim());
        int awayScore = Integer.parseInt(scoreParts[1].trim());

        return Goal.builder()
                .minute(mapMinute(goalDto))
                .goalBy(goalBy)
                .assistBy(assistBy)
                .teamName(teamName)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .teamType(teamType)
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
                .externalId(teamId)
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
}
