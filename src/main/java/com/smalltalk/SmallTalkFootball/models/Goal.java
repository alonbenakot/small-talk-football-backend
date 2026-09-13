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

    /** apifootball player ids (the same identifier as {@code PlayerData.id}); null when the feed sends a blank. */
    private String scorerId;

    private String assistId;

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
