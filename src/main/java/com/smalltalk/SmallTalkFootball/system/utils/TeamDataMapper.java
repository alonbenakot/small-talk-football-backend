package com.smalltalk.SmallTalkFootball.system.utils;

import com.smalltalk.SmallTalkFootball.domain.TeamData;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;

public class TeamDataMapper {

    public static TeamData map(TeamDataDto teamDto) {
        return TeamData.builder()
                .name(teamDto.getTeamName())
                .externalKey(teamDto.getTeamKey())
                .crest(teamDto.getTeamBadge())
                .coach(teamDto.getCoaches().get(0).getCoachName())
                .build();
    }
}
