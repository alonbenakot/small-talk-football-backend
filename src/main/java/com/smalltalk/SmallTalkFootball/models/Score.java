package com.smalltalk.SmallTalkFootball.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class Score {
    private String winner;

    private boolean draw;

    private int away;

    private int home;
}
