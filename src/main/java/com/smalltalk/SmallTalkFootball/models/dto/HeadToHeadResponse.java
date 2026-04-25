package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HeadToHeadResponse {

	@JsonProperty("firstTeam_lastResults")
	private List<SummaryMatchDto> firstTeamLastResults;

	@JsonProperty("secondTeam_lastResults")
	private List<SummaryMatchDto> secondTeamLastResults;

	@JsonProperty("firstTeam_VS_secondTeam")
	private List<SummaryMatchDto> firstTeamVSSecondTeam;
}