package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Team;
import com.smalltalk.SmallTalkFootball.models.dto.org.GoalDTO;
import com.smalltalk.SmallTalkFootball.models.dto.org.MatchesItemDTO;
import com.smalltalk.SmallTalkFootball.models.dto.org.ScoreDTO;
import com.smalltalk.SmallTalkFootball.models.dto.org.TeamDTO;

import java.util.List;
import java.util.Objects;

public class FixtureMapper {

    public static final String PENALTY = "PENALTY";

    public static Fixture map(MatchesItemDTO matchDTO) {
        return Fixture.builder()
                .externalId(matchDTO.getId())
                .competition(Competition.fromCode(matchDTO.getCompetition().getCode()))
                .venue(matchDTO.getVenue())
                .homeTeam(mapTeam(matchDTO.getHomeTeam()))
                .awayTeam(mapTeam(matchDTO.getAwayTeam()))
                .utcDate(matchDTO.getUtcDate())
                .durationInMinutes(matchDTO.getMinute() != null ? matchDTO.getMinute() : "90")
                .goals(mapGoals(matchDTO.getGoals()))
                .score(mapScore(matchDTO.getScore()))
                .build();
    }

    private static Score mapScore(ScoreDTO dto) {
        return Score.builder()
                .winner(dto.getWinner())
                .home(dto.getHome())
                .away(dto.getAway())
                .build();
    }

    private static List<Goal> mapGoals(List<GoalDTO> goalsDto) {
        if (goalsDto == null) return List.of();

        return goalsDto.stream()
                .map(dto -> Goal.builder()
                        .minute(dto.getMinute())
                        .scoreBy(dto.getScorer() != null ? dto.getScorer().getName() : null)
                        .assistBy(dto.getAssist() != null ? dto.getAssist().getName() : null)
                        .teamName(dto.getTeam() != null ? dto.getTeam().getName() : null)
                        .penalty(PENALTY.equalsIgnoreCase(dto.getType()))
                        .homeScore(dto.getScore() != null ? dto.getScore().getHome() : null)
                        .awayScore(dto.getScore() != null ? dto.getScore().getAway() : null)
                        .build())
                .toList();
    }

    private static Team mapTeam(TeamDTO dto) {
        return Team.builder()
                .id(dto.getId())
                .crest(dto.getCrest())
//                .leagueRank(dto.getLeagueRank())
                .name(dto.getName())
                .shortName(dto.getShortName())
                .coach(Objects.isNull(dto.getCoach()) ? null : dto.getName())
                .formation(Objects.isNull(dto.getFormation()) ? null : dto.getFormation())
                .build();
    }

}
