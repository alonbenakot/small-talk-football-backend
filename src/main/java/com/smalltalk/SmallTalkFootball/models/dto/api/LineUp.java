package com.smalltalk.SmallTalkFootball.models.dto.api;

import java.util.List;

public class HomeLinup {
	private List<StartingLineupsItem> startingLineups;
	private List<SubstitutesItem> substitutes;
	private List<CoachItem> coach;
	private List<Object> missingPlayers;

	public List<StartingLineupsItem> getStartingLineups(){
		return startingLineups;
	}

	public List<SubstitutesItem> getSubstitutes(){
		return substitutes;
	}

	public List<CoachItem> getCoaches(){
		return coach;
	}

	public List<Object> getMissingPlayers(){
		return missingPlayers;
	}
}