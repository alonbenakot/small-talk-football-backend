package com.smalltalk.SmallTalkFootball.system.utils.mappers;

import com.smalltalk.SmallTalkFootball.models.Venue;
import com.smalltalk.SmallTalkFootball.models.dto.TeamDataDto;
import com.smalltalk.SmallTalkFootball.testsupport.JsonFixtures;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;

class TeamDataUpdateMapperTest {

    private final TeamDataUpdateMapper mapper = new TeamDataUpdateMapper();

    private static TeamDataDto dto(String coachesJson) {
        return JsonFixtures.parse("""
                {
                  "team_key": "2621",
                  "team_name": "Liverpool",
                  "team_badge": "liverpool.png",
                  "coaches": %s
                }
                """.formatted(coachesJson), TeamDataDto.class);
    }

    private static Document setFields(Update update) {
        return update.getUpdateObject().get("$set", Document.class);
    }

    @Test
    void setsNameCrestAndCoach() {
        Update update = mapper.map(dto("[{\"coach_name\": \"Arne Slot\"}]"));

        assertThat(setFields(update))
                .containsEntry("name", "Liverpool")
                .containsEntry("crest", "liverpool.png")
                .containsEntry("coach", "Arne Slot");
    }

    @Test
    void takesTheFirstCoachWhenSeveralAreListed() {
        Update update = mapper.map(dto("[{\"coach_name\": \"Arne Slot\"}, {\"coach_name\": \"Assistant\"}]"));

        assertThat(setFields(update)).containsEntry("coach", "Arne Slot");
    }

    /*
     * An empty coach list is normal for national teams, so it maps to an empty string rather
     * than failing. Note this overwrites any coach already stored for the team.
     */
    @Test
    void usesAnEmptyCoachWhenNoneIsListed() {
        Update update = mapper.map(dto("[]"));

        assertThat(setFields(update)).containsEntry("coach", "");
    }

    @Test
    void doesNotTouchTheStandingsField() {
        Update update = mapper.map(dto("[{\"coach_name\": \"Arne Slot\"}]"));

        assertThat(setFields(update)).doesNotContainKey("standings");
    }

    @Test
    void bindsFoundedAndTheNestedVenueObject() {
        TeamDataDto team = JsonFixtures.parse("""
                {
                  "team_key": "80", "team_name": "Manchester City", "team_badge": "mc.png",
                  "team_founded": "1880", "coaches": [],
                  "venue": {
                    "venue_name": "Etihad Stadium", "venue_address": "Rowsley Street",
                    "venue_city": "Manchester", "venue_capacity": "55097", "venue_surface": "grass"
                  }
                }
                """, TeamDataDto.class);

        Update update = mapper.map(team);

        assertThat(setFields(update)).containsEntry("founded", "1880");

        Venue venue = (Venue) setFields(update).get("venue");
        assertThat(venue.getName()).isEqualTo("Etihad Stadium");
        assertThat(venue.getCity()).isEqualTo("Manchester");
        assertThat(venue.getCapacity()).isEqualTo("55097");
        assertThat(venue.getSurface()).isEqualTo("grass");
    }

    /**
     * A team payload can arrive without a venue (national sides, sparse leagues); the
     * mapper still sets the key so a stale venue is cleared rather than left behind.
     */
    @Test
    void setsANullVenueWhenThePayloadHasNone() {
        Update update = mapper.map(dto("[]"));

        assertThat(setFields(update)).containsKey("venue");
        assertThat(setFields(update).get("venue")).isNull();
    }
}
