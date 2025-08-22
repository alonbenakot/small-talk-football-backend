package com.smalltalk.SmallTalkFootball.models;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class Team {
    private int leagueRank;

    private String name;

    private String id;

    private String formation;

    private String shortName;

    private String coach;

    private String crest;
}
