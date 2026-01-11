package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;
import com.smalltalk.SmallTalkFootball.models.dto.HeadToHeadResponse;
import com.smalltalk.SmallTalkFootball.models.dto.SummaryMatchDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HeadToHeadMapper implements Mapper<HeadToHeadResponse, HeadToHeadData> {

    private final Mapper<SummaryMatchDto, Fixture> fixtureMapper;

    public HeadToHeadMapper(@Qualifier("summaryFixtureMapper") Mapper<SummaryMatchDto, Fixture> fixtureMapper) {
        this.fixtureMapper = fixtureMapper;
    }

    @Override
    public HeadToHeadData map(HeadToHeadResponse dto) {
        if (dto == null) {
            throw new IllegalArgumentException("HeadToHeadResponse cannot be null");
        }

        return HeadToHeadData.builder()
                .firstTeamLastFixtures(mapFixtures(dto.getFirstTeamLastResults()))
                .secondTeamLastFixtures(mapFixtures(dto.getSecondTeamLastResults()))
                .teamsLastFixtures(mapFixtures(dto.getFirstTeamVSSecondTeam()))
                .build();
    }

    private List<Fixture> mapFixtures(List<SummaryMatchDto> dtoList) {
        if (dtoList == null) {
            return List.of();
        }
        return dtoList.stream()
                .map(fixtureMapper::map)
                .toList();
    }
}
