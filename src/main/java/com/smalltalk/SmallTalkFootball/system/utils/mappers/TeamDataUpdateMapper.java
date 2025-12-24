package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import org.springframework.data.mongodb.core.query.Update;

public class TeamDataUpdateMapper {

    public static Update map(TeamDataDto teamDto) {
        return new Update()
                .set("name", teamDto.getTeamName())
                .set("crest", teamDto.getTeamBadge())
                .set("coach", teamDto.getCoaches().isEmpty() ? "" : teamDto.getCoaches().get(0).getCoachName());
    }
}
