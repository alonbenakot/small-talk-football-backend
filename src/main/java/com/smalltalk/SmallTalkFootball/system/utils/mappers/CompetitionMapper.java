package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.CompetitionData;
import com.smalltalk.SmallTalkFootball.models.dto.CompetitionDto;

public class CompetitionMapper {

    public static CompetitionData map(CompetitionDto dto) {
        return CompetitionData.builder()
                .countryName(dto.getCountryName())
                .leagueId(Integer.parseInt(dto.getLeagueId()))
                .leagueLogo(dto.getLeagueLogo())
                .leagueSeason(dto.getLeagueSeason())
                .leagueName(dto.getLeagueName())
                .build();
    }
}
