package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class HomeTeam{
	private List<Object> bench;
	private int leagueRank;
	private String name;
	private String tla;
	private List<Object> lineup;
	private int id;
	private String formation;
	private String shortName;
	private Coach coach;
	private String crest;

	public List<Object> getBench(){
		return bench;
	}

	public int getLeagueRank(){
		return leagueRank;
	}

	public String getName(){
		return name;
	}

	public String getTla(){
		return tla;
	}

	public List<Object> getLineup(){
		return lineup;
	}

	public int getId(){
		return id;
	}

	public String getFormation(){
		return formation;
	}

	public String getShortName(){
		return shortName;
	}

	public Coach getCoach(){
		return coach;
	}

	public String getCrest(){
		return crest;
	}
}