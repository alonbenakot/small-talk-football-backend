package com.smalltalk.SmallTalkFootball.models;

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
}
