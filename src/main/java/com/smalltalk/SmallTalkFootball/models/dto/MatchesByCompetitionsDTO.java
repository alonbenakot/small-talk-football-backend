package com.smalltalk.SmallTalkFootball.models;

import java.util.List;

public class MatchesByCompetitionsDTO{
	private Competition competition;
	private Filters filters;
	private List<MatchesItem> matches;
	private ResultSet resultSet;

	public Competition getCompetition(){
		return competition;
	}

	public Filters getFilters(){
		return filters;
	}

	public List<MatchesItem> getMatches(){
		return matches;
	}

	public ResultSet getResultSet(){
		return resultSet;
	}
}