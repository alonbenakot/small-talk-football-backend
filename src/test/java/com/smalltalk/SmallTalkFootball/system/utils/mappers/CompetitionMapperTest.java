package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.models.dto.CompetitionDto;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetitionMapperTest {

    private static CompetitionDto dto(String leagueId) {
        return JsonFixtures.parse("""
                {
                  "league_id": "%s",
                  "league_name": "Premier League",
                  "league_logo": "premier-league.png",
                  "league_season": "2025/2026",
                  "country_name": "England",
                  "country_id": "41",
                  "country_logo": "england.png"
                }
                """.formatted(leagueId), CompetitionDto.class);
    }

    @Test
    void copiesCompetitionDetailAndParsesTheLeagueIdIntoTheDocumentId() {
        CompetitionData competition = CompetitionMapper.map(dto("152"));

        assertThat(competition.getLeagueId()).isEqualTo(152);
        assertThat(competition.getLeagueName()).isEqualTo("Premier League");
        assertThat(competition.getLeagueLogo()).isEqualTo("premier-league.png");
        assertThat(competition.getLeagueSeason()).isEqualTo("2025/2026");
        assertThat(competition.getCountryName()).isEqualTo("England");
    }

    @Test
    void throwsOnANonNumericLeagueId() {
        CompetitionDto nonNumeric = dto("not-a-number");

        assertThatThrownBy(() -> CompetitionMapper.map(nonNumeric))
                .isInstanceOf(NumberFormatException.class);
    }
}
