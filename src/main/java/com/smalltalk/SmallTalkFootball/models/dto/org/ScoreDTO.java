package com.smalltalk.SmallTalkFootball.models.dto;

public class ScoreDTO {
	private String duration;
	private String winner;
	private HalfTime halfTime;
	private FullTime fullTime;
	private int away;
	private int home;

	public String getDuration(){
		return duration;
	}

	public String getWinner(){
		return winner;
	}

	public HalfTime getHalfTime(){
		return halfTime;
	}

	public FullTime getFullTime(){
		return fullTime;
	}

	public int getAway(){
		return away;
	}

	public int getHome(){
		return home;
	}
}
