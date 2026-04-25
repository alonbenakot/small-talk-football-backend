package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.models.dto.MatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Qualifier("fullFixtureMapper")
public class FullFixtureMapper implements  Mapper<MatchDto, Fixture>{

    private final FixtureAssembler assembler;

    @Override
    public Fixture map(MatchDto matchDto) {
        return assembler.assembleFromFullMatch(matchDto);
    }
}
