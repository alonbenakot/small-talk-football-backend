package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.models.dto.SummaryMatchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Qualifier("summaryFixtureMapper")
public class SummaryFixtureMapper implements Mapper<SummaryMatchDto, Fixture> {

    private FixtureAssembler assembler;

    @Override
    public Fixture map(SummaryMatchDto summaryMatchDto) {
        return assembler.assembleFromSummary(summaryMatchDto);
    }
}
