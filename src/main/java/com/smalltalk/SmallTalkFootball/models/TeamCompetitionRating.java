package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamCompetitionRating {

    private String externalTeamId;

    private Competition competition;

    private Integer position;

    private Integer playedMatches;

    private WinLossDraw overall;

    private WinLossDraw home;

    private WinLossDraw away;

}
