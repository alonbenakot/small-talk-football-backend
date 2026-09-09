package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;
import com.smalltalk.SmallTalkFootball.models.dto.HeadToHeadResponse;
import com.smalltalk.SmallTalkFootball.models.dto.SummaryMatchDto;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeadToHeadMapperTest {

    private final HeadToHeadMapper mapper =
            new HeadToHeadMapper(new SummaryFixtureMapper(new FixtureAssembler()));

    private static SummaryMatchDto summary(String homeName, String awayName, String homeScore, String awayScore) {
        SummaryMatchDto dto = new SummaryMatchDto();
        dto.setMatchId("7001");
        dto.setLeagueId("152");
        dto.setMatchStatus("Finished");
        dto.setMatchHometeamId("2621");
        dto.setMatchAwayteamId("2622");
        dto.setMatchHometeamName(homeName);
        dto.setMatchAwayteamName(awayName);
        dto.setMatchHometeamScore(homeScore);
        dto.setMatchAwayteamScore(awayScore);
        return dto;
    }

    @Test
    void mapsAllThreeResultListsIntoFixtures() {
        HeadToHeadResponse response = new HeadToHeadResponse();
        response.setFirstTeamLastResults(List.of(summary("Liverpool", "Arsenal", "2", "0")));
        response.setSecondTeamLastResults(List.of(
                summary("Everton", "Chelsea", "1", "1"),
                summary("Everton", "Spurs", "0", "3")));
        response.setFirstTeamVSSecondTeam(List.of(summary("Liverpool", "Everton", "2", "1")));

        HeadToHeadData data = mapper.map(response);

        assertThat(data.getFirstTeamLastFixtures()).hasSize(1);
        assertThat(data.getSecondTeamLastFixtures()).hasSize(2);
        assertThat(data.getTeamsLastFixtures()).hasSize(1);
        assertThat(data.getTeamsLastFixtures().get(0).getScore().getWinner()).isEqualTo("Liverpool");
    }

    @Test
    void treatsMissingResultListsAsEmptyRatherThanNull() {
        HeadToHeadData data = mapper.map(new HeadToHeadResponse());

        assertThat(data.getFirstTeamLastFixtures()).isEmpty();
        assertThat(data.getSecondTeamLastFixtures()).isEmpty();
        assertThat(data.getTeamsLastFixtures()).isEmpty();
    }

    @Test
    void rejectsANullResponse() {
        assertThatThrownBy(() -> mapper.map(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void bindsTheApifootballFieldNamesOnTheWayIn() {
        HeadToHeadResponse response = JsonFixtures.parse("""
                {
                  "firstTeam_lastResults": [
                    {"match_id":"1","league_id":"152","match_status":"Finished",
                     "match_hometeam_name":"Liverpool","match_awayteam_name":"Arsenal",
                     "match_hometeam_score":"2","match_awayteam_score":"0",
                     "match_hometeam_id":"2621","match_awayteam_id":"2600"}
                  ],
                  "secondTeam_lastResults": [],
                  "firstTeam_VS_secondTeam": []
                }
                """, HeadToHeadResponse.class);

        HeadToHeadData data = mapper.map(response);

        assertThat(data.getFirstTeamLastFixtures()).hasSize(1);
        assertThat(data.getFirstTeamLastFixtures().get(0).getHomeTeam().getName()).isEqualTo("Liverpool");
        assertThat(data.getFirstTeamLastFixtures().get(0).getScore().getWinner()).isEqualTo("Liverpool");
    }
}
