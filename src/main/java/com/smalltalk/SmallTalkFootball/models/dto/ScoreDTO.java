package com.smalltalk.SmallTalkFootball.models.dto;

import com.smalltalk.SmallTalkFootball.models.dto.FullTime;
import com.smalltalk.SmallTalkFootball.models.dto.HalfTime;

public class Score{
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
