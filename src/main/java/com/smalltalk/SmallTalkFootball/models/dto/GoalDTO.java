package com.smalltalk.SmallTalkFootball.models.dto;

public class GoalsItem{
	private ScoreDTO score;
	private Object injuryTime;
	private Scorer scorer;
	private Object assist;
	private Team team;
	private String type;
	private int minute;

	public ScoreDTO getScore(){
		return score;
	}

	public Object getInjuryTime(){
		return injuryTime;
	}

	public Scorer getScorer(){
		return scorer;
	}

	public Object getAssist(){
		return assist;
	}

	public Team getTeam(){
		return team;
	}

	public String getType(){
		return type;
	}

	public int getMinute(){
		return minute;
	}
}
