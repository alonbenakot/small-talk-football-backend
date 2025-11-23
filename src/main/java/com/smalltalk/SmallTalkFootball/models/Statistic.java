package com.smalltalk.SmallTalkFootball.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class Statistic {
    String type;
    Integer home;
    Integer away;
    boolean percentage;
}
