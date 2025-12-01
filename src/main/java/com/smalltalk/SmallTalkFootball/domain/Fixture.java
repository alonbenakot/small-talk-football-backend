package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.models.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

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

    private List<Goal> goals = new ArrayList<>();

    private int durationInMinutes;

    private int externalId;

    private Instant matchDateTime;

    private boolean finished;

    private List<Statistic> statistics = new ArrayList<>();

    private Set<OneLiner> oneLiners = new HashSet<>();

    public Set<OneLiner> getOneLiners() {
        return oneLiners == null ? Collections.emptySet()
                : Collections.unmodifiableSet(oneLiners);
    }

    /**
     * Add a one-liner only if it does not already exist for this team/language.
     * Existing entries remain untouched.
     */
    public boolean addOneLiner(OneLiner oneLiner) {
        return oneLiners.add(oneLiner);
    }

    /**
     * Replace any existing one-liner for this team/language with the new one.
     * Always ensures the Set contains the latest.
     */
    public void replaceOneLiner(OneLiner oneLiner) {
        oneLiners.remove(oneLiner);
        oneLiners.add(oneLiner);
    }

}
