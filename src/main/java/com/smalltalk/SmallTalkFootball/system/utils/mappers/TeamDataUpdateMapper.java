package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.models.Venue;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.models.dto.VenueDto;
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
                .set("coach", teamDto.getCoaches().isEmpty() ? "" : teamDto.getCoaches().get(0).getCoachName())
                .set("founded", teamDto.getTeamFounded())
                .set("venue", mapVenue(teamDto.getVenue()));
    }

    private Venue mapVenue(VenueDto venue) {
        if (venue == null) {
            return null;
        }
        return Venue.builder()
                .name(venue.getVenueName())
                .address(venue.getVenueAddress())
                .city(venue.getVenueCity())
                .capacity(venue.getVenueCapacity())
                .surface(venue.getVenueSurface())
                .build();
    }
}
