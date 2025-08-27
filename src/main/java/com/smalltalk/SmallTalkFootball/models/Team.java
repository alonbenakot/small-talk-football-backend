package com.smalltalk.SmallTalkFootball.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class Team {
    private String name;

    private String externalId;

    private String formation;

    private String coach;

    private String crest;
}
