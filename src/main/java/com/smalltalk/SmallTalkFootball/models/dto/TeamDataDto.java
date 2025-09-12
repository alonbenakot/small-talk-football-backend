package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class TeamDataDto {
	private List<CoachesItem> coaches;
	private String teamKey;
	private String teamName;
	private String teamBadge;

	public List<CoachesItem> getCoaches(){
		return coaches;
	}

	public String getTeamKey(){
		return teamKey;
	}

	public String getTeamName(){
		return teamName;
	}

	public String getTeamBadge(){
		return teamBadge;
	}
}