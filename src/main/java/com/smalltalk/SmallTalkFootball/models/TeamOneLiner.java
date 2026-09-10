package com.smalltalk.SmallTalkFootball.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * A cached team one-liner, held in a {@code Set} on
 * {@link com.smalltalk.SmallTalkFootball.domain.TeamData}. Like {@link OneLiner} it keys on
 * everything except its own text, so the set holds exactly one sentence per language,
 * competition and perspective and a regeneration replaces rather than duplicates.
 * <p>
 * There is no TTL. The standings snapshot taken at generation time is what makes staleness
 * detectable when the team itself has not played: a position or points move by another club
 * is enough to invalidate "top of the table".
 */
@Getter
@Builder
@AllArgsConstructor
public class TeamOneLiner {

    Language language;

    Competition competition;

    Perspective perspective;

    String text;

    Instant generatedAt;

    /** Snapshot of the standing this sentence was written against; not part of the response. */
    @JsonIgnore
    Integer positionAtGeneration;

    @JsonIgnore
    Integer pointsAtGeneration;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TeamOneLiner other = (TeamOneLiner) obj;
        return Objects.equals(language, other.language) &&
                Objects.equals(competition, other.competition) &&
                Objects.equals(perspective, other.perspective);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, competition, perspective);
    }
}
