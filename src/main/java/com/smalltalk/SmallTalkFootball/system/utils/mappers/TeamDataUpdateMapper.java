package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@Qualifier("teamDataUpdateMapper")
public class TeamDataUpdateMapper implements Mapper<TeamDataDto, Update> {

    public Update map(TeamDataDto teamDto) {
        return new Update()
                .set("name", teamDto.getTeamName())
                .set("crest", teamDto.getTeamBadge())
                .set("coach", teamDto.getCoaches().isEmpty() ? "" : teamDto.getCoaches().get(0).getCoachName());
    }
}
