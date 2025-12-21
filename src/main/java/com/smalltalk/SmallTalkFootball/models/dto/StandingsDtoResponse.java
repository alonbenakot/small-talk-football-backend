package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class StandingsDtoResponse {
	private List<StandingsDtoItem> standingsDtO;

	public List<StandingsDtoItem> getStandingsDtO(){
		return standingsDtO;
	}

	@Override
 	public String toString(){
		return 
			"StandingsDtO{" + 
			"standingsDtO = '" + standingsDtO + '\'' + 
			"}";
		}
}