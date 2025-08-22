package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class TeamDataDto {
	private List<CoachesItem> coaches;
	private Venue venue;
	private String teamCountry;
	private String teamFounded;
	private List<PlayersItem> players;
	private String teamKey;
	private String teamName;
	private String teamBadge;

	public List<CoachesItem> getCoaches(){
		return coaches;
	}

	public Venue getVenue(){
		return venue;
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