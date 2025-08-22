package com.smalltalk.SmallTalkFootball.models.dto;

public class MatchLineup {
	private LineUp away;
	private LineUp home;

	public LineUp getAwayLineUp(){
		return away;
	}

	public LineUp getHomeLineUp(){
		return home;
	}
}
