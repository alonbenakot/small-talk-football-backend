package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionData {

    @Id
    private int leagueId;

    private String leagueName;

    private String leagueLogo;

    private String countryName;

    private String leagueSeason;

    /** Serialised by Jackson only (Spring Data maps fields), so the FE can key its tabs on the enum name. */
    public Competition getCompetition() {
        return Competition.fromCode(leagueId);
    }
}
