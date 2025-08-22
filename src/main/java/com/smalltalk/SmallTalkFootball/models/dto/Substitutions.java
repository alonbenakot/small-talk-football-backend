package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class Substitutions{
	private List<AwayItem> away;
	private List<HomeItem> home;

	public List<AwayItem> getAway(){
		return away;
	}

	public List<HomeItem> getHome(){
		return home;
	}
}