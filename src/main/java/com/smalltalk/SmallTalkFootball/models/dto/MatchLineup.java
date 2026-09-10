package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchLineup {
	@JsonProperty("away")
	private LineUp away;
	@JsonProperty("home")
	private LineUp home;

	public LineUp getAwayLineUp(){
		return away;
	}

	public LineUp getHomeLineUp(){
		return home;
	}
}
