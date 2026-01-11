package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;
import com.smalltalk.SmallTalkFootball.models.dto.HeadToHeadResponse;
import com.smalltalk.SmallTalkFootball.services.FootballApiService;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilderFactory {

    private final Mapper<HeadToHeadResponse, HeadToHeadData> headToHeadMapper;

    private final FootballApiService footballApiService;

    public PromptBuilderFactory(@Qualifier("headToHeadMapper") Mapper<HeadToHeadResponse, HeadToHeadData> headToHeadMapper,
                                FootballApiService footballApiService) {
        this.headToHeadMapper = headToHeadMapper;
        this.footballApiService = footballApiService;
    }

    public PromptBuilder create(Fixture fixture, TeamType teamType, Language language) {
        if (fixture.isFinished()) {
            return new FinishedFixtureOneLinerPromptBuilder(fixture, teamType, language);
        }

        String homeTeamId = fixture.getHomeTeam().getExternalId();
        String awayTeamId = fixture.getAwayTeam().getExternalId();

        HeadToHeadData headToHeadData = footballApiService.
                getHeadToHeadData(homeTeamId, awayTeamId)
                .map(headToHeadMapper::map)
                .orElseThrow(() -> new IllegalStateException("Missing head-to-head data for fixture " + fixture.getId()));

        return new UpcomingFixtureOneLinerPromptBuilder(fixture, teamType, language, headToHeadData);

    }

}
