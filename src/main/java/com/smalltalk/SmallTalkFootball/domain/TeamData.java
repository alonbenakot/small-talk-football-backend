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

    /**
     * The competition a sentence about this team is about: the domestic league — the standing
     * that is neither the Champions League nor the World Cup — and the most-played one if there
     * is still a choice. Empty for a side with no standing outside the World Cup.
     */
    public Optional<Competition> primaryCompetition() {
        List<Competition> candidates = standings == null ? List.of() : standings.keySet().stream()
                .filter(competition -> competition != Competition.WORLD_CUP)
                .toList();
        List<Competition> domestic = candidates.stream()
                .filter(competition -> competition != Competition.CHAMPIONS_LEAGUE)
                .toList();

        return (domestic.isEmpty() ? candidates : domestic).stream()
                .max(Comparator.comparingInt(this::playedMatches));
    }

    private int playedMatches(Competition competition) {
        Standing standing = standings.get(competition);
        return standing == null || standing.getPlayedMatches() == null ? 0 : standing.getPlayedMatches();
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
