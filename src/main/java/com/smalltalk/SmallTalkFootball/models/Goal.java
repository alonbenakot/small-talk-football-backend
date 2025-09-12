package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.TeamType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Goal {
    private String goalBy;

    private String assistBy;

    private int homeScore;

    private int awayScore;

    private String teamName;

    private boolean penalty;

    private int minute;

    private TeamType teamType;

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
