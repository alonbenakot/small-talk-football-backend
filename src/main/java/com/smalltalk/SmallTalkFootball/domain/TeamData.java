package com.smalltalk.SmallTalkFootball.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.models.Standing;
import com.smalltalk.SmallTalkFootball.models.TeamOneLiner;
import com.smalltalk.SmallTalkFootball.models.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

@Builder
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamData {

    @Id
    private String id;

    private String name;

    private String coach;

    private String crest;

    private String founded;

    private Venue venue;

    private Map<Competition, Standing> standings;

    /**
     * Cached team one-liners, one per language/competition/perspective. {@code @Builder.Default}
     * is load-bearing: without it a builder-made TeamData carries a null set and
     * {@link #addOneLiner} would throw.
     */
    @JsonIgnore
    @Builder.Default
    private Set<TeamOneLiner> oneLiners = new HashSet<>();

    public Set<TeamOneLiner> getOneLiners() {
        return oneLiners == null ? Collections.emptySet()
                : Collections.unmodifiableSet(oneLiners);
    }

    public Optional<TeamOneLiner> findOneLiner(Language language, Competition competition, Perspective perspective) {
        return getOneLiners().stream()
                .filter(oneLiner -> oneLiner.getLanguage() == language
                        && oneLiner.getCompetition() == competition
                        && oneLiner.getPerspective() == perspective)
                .findAny();
    }

    /**
     * Add a one-liner only if none exists yet for this language/competition/perspective.
     * Existing entries remain untouched.
     */
    public boolean addOneLiner(TeamOneLiner oneLiner) {
        return oneLiners.add(oneLiner);
    }

    /**
     * Replace any existing one-liner for this language/competition/perspective with the new
     * one. Regeneration must use this: equality ignores the text, so {@code add} alone would
     * be a silent no-op and the stale sentence would survive.
     */
    public void replaceOneLiner(TeamOneLiner oneLiner) {
        oneLiners.remove(oneLiner);
        oneLiners.add(oneLiner);
    }
}
