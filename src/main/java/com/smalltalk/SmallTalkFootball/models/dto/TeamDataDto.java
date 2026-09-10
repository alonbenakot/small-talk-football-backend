package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TeamDataDto {
	private List<CoachesItem> coaches;
	private String teamKey;
	private String teamName;
	private String teamBadge;

	// Added for the team one-liner feature. The getter names on this DTO drift from their
	// fields (see bug #3), and the apifootball mapper is the SNAKE_CASE one which does not
	// run an explicit @JsonProperty value through the naming strategy, so every new field
	// carries its verbatim wire name.
	@JsonProperty("team_founded")
	private String teamFounded;

	@JsonProperty("team_country")
	private String teamCountry;

	@JsonProperty("venue")
	private VenueDto venue;

	@JsonProperty("players")
	private List<PlayerItem> players;

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

	public String getTeamFounded(){
		return teamFounded;
	}

	public String getTeamCountry(){
		return teamCountry;
	}

	public VenueDto getVenue(){
		return venue;
	}

	public List<PlayerItem> getPlayers(){
		return players;
	}
}
