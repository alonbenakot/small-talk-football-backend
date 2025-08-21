package com.smalltalk.SmallTalkFootball.models.dto;

public class GoalDTO {
	private GoalScoreDTO score;
	private Object injuryTime;
	private Scorer scorer;
	private AssistDTO assist;
	private Team team;
	private String type;
	private int minute;

	public GoalScoreDTO getScore(){
		return score;
	}

	public Object getInjuryTime(){
		return injuryTime;
	}

	public Scorer getScorer(){
		return scorer;
	}

	public AssistDTO getAssist(){
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

	public class AssistDTO {
		private int id;

		private String name;

		public String getName() {
			return name;
		}
	}

	public class GoalScoreDTO {
		private int home;

		private int away;

		public int getHome() {
			return home;
		}

		public int getAway() {
			return away;
		}
	}
}
