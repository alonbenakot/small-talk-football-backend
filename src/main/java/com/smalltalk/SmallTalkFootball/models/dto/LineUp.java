package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LineUp {
	@JsonProperty("coach")
	private List<CoachItem> coach;

	public List<CoachItem> getCoaches(){
		return coach;
	}

}