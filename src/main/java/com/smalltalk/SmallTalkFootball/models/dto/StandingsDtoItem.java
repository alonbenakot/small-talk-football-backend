package com.smalltalk.SmallTalkFootball.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class StandingsDtoItem {
	@JsonProperty("away_league_W")
	private String awayLeagueW;
	@JsonProperty("overall_league_D")
	private String overallLeagueD;
	private String homeLeaguePTS;
	private String overallLeaguePayed;
	private String fkStageKey;
	@JsonProperty("overall_league_L")
	private String overallLeagueL;
	private String teamId;
	private String teamBadge;
	private String homeLeagueGF;
	private String homeLeaguePosition;
	@JsonProperty("away_league_L")
	private String awayLeagueL;
	private String awayLeaguePayed;
	private String homeLeagueGA;
	private String stageName;
	private String countryName;
	private String overallPromotion;
	private String overallLeagueGA;
	private String overallLeaguePosition;
	@JsonProperty("home_league_W")
	private String homeLeagueW;
	private String overallLeagueGF;
	@JsonProperty("away_league_D")
	private String awayLeagueD;
	@JsonProperty("overall_league_W")
	private String overallLeagueW;
	private String homeLeaguePayed;
	@JsonProperty("home_league_L")
	private String homeLeagueL;
	private String leagueRound;
	private String awayPromotion;
	private String homePromotion;
	private String leagueName;
	@JsonProperty("home_league_D")
	private String homeLeagueD;
	private String teamName;
	private String overallLeaguePTS;
	private String awayLeagueGF;
	private String awayLeagueGA;
	private String awayLeaguePosition;
	private String awayLeaguePTS;
	private String leagueId;

	@Override
 	public String toString(){
		return 
			"StandingsDtOItem{" + 
			"away_league_W = '" + awayLeagueW + '\'' + 
			",overall_league_D = '" + overallLeagueD + '\'' + 
			",home_league_PTS = '" + homeLeaguePTS + '\'' + 
			",overall_league_payed = '" + overallLeaguePayed + '\'' + 
			",fk_stage_key = '" + fkStageKey + '\'' + 
			",overall_league_L = '" + overallLeagueL + '\'' + 
			",team_id = '" + teamId + '\'' + 
			",team_badge = '" + teamBadge + '\'' + 
			",home_league_GF = '" + homeLeagueGF + '\'' + 
			",home_league_position = '" + homeLeaguePosition + '\'' + 
			",away_league_L = '" + awayLeagueL + '\'' + 
			",away_league_payed = '" + awayLeaguePayed + '\'' + 
			",home_league_GA = '" + homeLeagueGA + '\'' + 
			",stage_name = '" + stageName + '\'' + 
			",country_name = '" + countryName + '\'' + 
			",overall_promotion = '" + overallPromotion + '\'' + 
			",overall_league_GA = '" + overallLeagueGA + '\'' + 
			",overall_league_position = '" + overallLeaguePosition + '\'' + 
			",home_league_W = '" + homeLeagueW + '\'' + 
			",overall_league_GF = '" + overallLeagueGF + '\'' + 
			",away_league_D = '" + awayLeagueD + '\'' + 
			",overall_league_W = '" + overallLeagueW + '\'' + 
			",home_league_payed = '" + homeLeaguePayed + '\'' + 
			",home_league_L = '" + homeLeagueL + '\'' + 
			",league_round = '" + leagueRound + '\'' + 
			",away_promotion = '" + awayPromotion + '\'' + 
			",home_promotion = '" + homePromotion + '\'' + 
			",league_name = '" + leagueName + '\'' + 
			",home_league_D = '" + homeLeagueD + '\'' + 
			",team_name = '" + teamName + '\'' + 
			",overall_league_PTS = '" + overallLeaguePTS + '\'' + 
			",away_league_GF = '" + awayLeagueGF + '\'' + 
			",away_league_GA = '" + awayLeagueGA + '\'' + 
			",away_league_position = '" + awayLeaguePosition + '\'' + 
			",away_league_PTS = '" + awayLeaguePTS + '\'' + 
			",league_id = '" + leagueId + '\'' + 
			"}";
		}
}
