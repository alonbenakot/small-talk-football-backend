package com.smalltalk.SmallTalkFootball.models;

public class PenaltiesItem{
	private boolean scored;
	private Team team;
	private Player player;

	public boolean isScored(){
		return scored;
	}

	public Team getTeam(){
		return team;
	}

	public Player getPlayer(){
		return player;
	}
}
