package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.Goal;
import com.smalltalk.SmallTalkFootball.models.Score;
import com.smalltalk.SmallTalkFootball.models.Team;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Builder
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fixture {

    @Id
    private String id;

    private String venue;

    private Competition competition;

    private Score score;

    private Team homeTeam;

    private Team awayTeam;

    private List<Goal> goals;

    private String durationInMinutes;

    private int externalId;

    private String utcDate;

    private String matchStartTime;

}
