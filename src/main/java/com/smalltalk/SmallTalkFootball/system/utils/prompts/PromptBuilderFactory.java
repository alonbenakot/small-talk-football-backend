package com.smalltalk.SmallTalkFootball.system.utils.prompts;

import com.smalltalk.SmallTalkFootball.domain.Fixture;
import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.HeadToHeadData;
import com.smalltalk.SmallTalkFootball.models.dto.HeadToHeadResponse;
import com.smalltalk.SmallTalkFootball.services.FootballApiService;
import com.smalltalk.SmallTalkFootball.services.TeamDataService;
import com.smalltalk.SmallTalkFootball.system.exceptions.SmallTalkException;
import com.smalltalk.SmallTalkFootball.system.utils.mappers.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PromptBuilderFactory {

    private final Mapper<HeadToHeadResponse, HeadToHeadData> headToHeadMapper;

    private final FootballApiService footballApiService;

    private final TeamDataService teamDataService;

    public PromptBuilderFactory(@Qualifier("headToHeadMapper") Mapper<HeadToHeadResponse, HeadToHeadData> headToHeadMapper,
                                FootballApiService footballApiService,
                                TeamDataService teamDataService) {
        this.headToHeadMapper = headToHeadMapper;
        this.footballApiService = footballApiService;
        this.teamDataService = teamDataService;
    }

    public PromptBuilder create(Fixture fixture, TeamType teamType, Language language) throws SmallTalkException {
        if (fixture.getCompetition() == Competition.WORLD_CUP) {
            fixture.getHomeTeam().setCoach(null);
            fixture.getAwayTeam().setCoach(null);
        }

        if (fixture.isFinished()) {
            return new FinishedFixtureOneLinerPromptBuilder(fixture, teamType, language);
        }

        String homeTeamId = fixture.getHomeTeam().getId();
        String awayTeamId = fixture.getAwayTeam().getId();

        HeadToHeadData headToHeadData = footballApiService.
                getHeadToHeadData(homeTeamId, awayTeamId)
                .map(headToHeadMapper::map)
                .orElseThrow(() -> new IllegalStateException("Missing head-to-head data for fixture " + fixture.getId()));

        TeamData homeTeamData = teamDataService.getTeamById(homeTeamId);
        TeamData awayTeamData = teamDataService.getTeamById(awayTeamId);

        return new UpcomingFixtureOneLinerPromptBuilder(fixture, teamType, language, headToHeadData, homeTeamData, awayTeamData);
    }

    /**
     * The team-flavoured variant. Everything it needs is already gathered by the caller, so
     * unlike the fixture overload this fetches nothing — it exists here only so prompt
     * variants stay in one place.
     */
    public PromptBuilder create(TeamPromptContext context, Language language, Perspective perspective) {
        return new TeamOneLinerPromptBuilder(context, language, perspective);
    }

}
