package com.smalltalk.SmallTalkFootball.models.dto;

import java.util.List;

public class MatchesItemDTO {
	private Area area;
	private String venue;
	private List<PenaltiesItem> penalties;
	private int matchday;
	private TeamDTO awayTeam;
	private CompetitionDTO competition;
	private String utcDate;
	private String minute;
	private String lastUpdated;
	private ScoreDTO score;
	private int injuryTime;
	private String stage;
	private List<Object> substitutions;
	private Odds odds;
	private Season season;
	private TeamDTO homeTeam;
	private int id;
	private List<Object> bookings;
	private List<RefereesItem> referees;
	private Object attendance;
	private String status;
	private Object group;
	private List<GoalDTO> goals;

	public Area getArea(){
		return area;
	}

	public String getVenue(){
		return venue;
	}

	public List<PenaltiesItem> getPenalties(){
		return penalties;
	}

	public int getMatchday(){
		return matchday;
	}

	public TeamDTO getAwayTeam(){
		return awayTeam;
	}

	public CompetitionDTO getCompetition(){
		return competition;
	}

	public String getUtcDate(){
		return utcDate;
	}

	public String getMinute(){
		return minute;
	}

	public String getLastUpdated(){
		return lastUpdated;
	}

	public ScoreDTO getScore(){
		return score;
	}

	public int getInjuryTime(){
		return injuryTime;
	}

	public String getStage(){
		return stage;
	}

	public List<Object> getSubstitutions(){
		return substitutions;
	}

	public Odds getOdds(){
		return odds;
	}

	public Season getSeason(){
		return season;
	}

	public TeamDTO getHomeTeam(){
		return homeTeam;
	}

	public int getId(){
		return id;
	}

	public List<Object> getBookings(){
		return bookings;
	}

	public List<RefereesItem> getReferees(){
		return referees;
	}

	public Object getAttendance(){
		return attendance;
	}

	public String getStatus(){
		return status;
	}

	public Object getGroup(){
		return group;
	}

	public List<GoalDTO> getGoals(){
		return goals;
	}
}