package com.smalltalk.SmallTalkFootball.system.utils.mappers;

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
}
