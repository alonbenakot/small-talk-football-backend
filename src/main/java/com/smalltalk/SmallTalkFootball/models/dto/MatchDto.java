package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class MatchDto {
	private String matchAwayteamName;
	private MatchLineup lineup;
	private String matchDate;
	private String matchHometeamSystem;
	private String matchAwayteamScore;
	private String matchAwayteamId;
	private String teamHomeBadge;
	private String matchStadium;
	private List<GoalscorerItem> goalscorer;
	private String matchHometeamId;
	private String matchId;
	private String matchTime;
	private String matchAwayteamSystem;
	private String matchStatus;
	private String matchHometeamScore;
	private String matchHometeamName;
	private String leagueId;
	private String teamAwayBadge;

	public String getMatchAwayTeamName(){
		return matchAwayteamName;
	}

	public MatchLineup getMatchLineup(){
		return lineup;
	}

	public String getMatchDate(){
		return matchDate;
	}

	public String getMatchHometeamSystem(){
		return matchHometeamSystem;
	}

	public String getMatchAwayteamScore(){
		return matchAwayteamScore;
	}

	public String getMatchAwayteamId(){
		return matchAwayteamId;
	}

	public String getTeamHomeBadge(){
		return teamHomeBadge;
	}

	public String getMatchStadium(){
		return matchStadium;
	}

	public List<GoalscorerItem> getGoalscorer(){
		return goalscorer;
	}

	public String getMatchHometeamId(){
		return matchHometeamId;
	}

	public String getMatchId(){
		return matchId;
	}

	public String getMatchTime(){
		return matchTime;
	}

	public String getMatchAwayteamSystem(){
		return matchAwayteamSystem;
	}

	public String getMatchStatus(){
		return matchStatus;
	}

	public String getMatchHometeamScore(){
		return matchHometeamScore;
	}

	public String getMatchHomeTeamName(){
		return matchHometeamName;
	}

	public String getLeagueId(){
		return leagueId;
	}

	public String getTeamAwayBadge(){
		return teamAwayBadge;
	}

}