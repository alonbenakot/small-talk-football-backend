package com.smalltalk.SmallTalkFootball.models.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class SubstitutionsDto {
    private List<TeamSubstitutions> awaySubstitutions;
    private List<TeamSubstitutions> homeSubstitutions;
}