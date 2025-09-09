package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class TeamDataDto {
	//TODO cleanup unused fields
	private List<CoachesItem> coaches;
	private String teamCountry;
	private String teamFounded;
	private List<PlayersItem> players;
	private String teamKey;
	private String teamName;
	private String teamBadge;

	public List<CoachesItem> getCoaches(){
		return coaches;
	}

	public String getTeamCountry(){
		return teamCountry;
	}

	public String getTeamFounded(){
		return teamFounded;
	}

	public List<PlayersItem> getPlayers(){
		return players;
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