package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class Season{
	private Object winner;
	private int currentMatchday;
	private String endDate;
	private List<String> stages;
	private int id;
	private String startDate;

	public Object getWinner(){
		return winner;
	}

	public int getCurrentMatchday(){
		return currentMatchday;
	}

	public String getEndDate(){
		return endDate;
	}

	public List<String> getStages(){
		return stages;
	}

	public int getId(){
		return id;
	}

	public String getStartDate(){
		return startDate;
	}
}