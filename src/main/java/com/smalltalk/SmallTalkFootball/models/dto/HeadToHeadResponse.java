package com.smalltalk.SmallTalkFootball.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class HeadToHeadResponse{
	private List<SummaryMatchDto> firstTeamLastResults;
	private List<SummaryMatchDto> secondTeamLastResults;
	private List<SummaryMatchDto> firstTeamVSSecondTeam;
}