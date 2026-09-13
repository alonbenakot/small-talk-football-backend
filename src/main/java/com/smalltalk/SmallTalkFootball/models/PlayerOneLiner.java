package com.smalltalk.SmallTalkFootball.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smalltalk.SmallTalkFootball.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * A cached player one-liner, held in a {@code Set} on
 * {@link com.smalltalk.SmallTalkFootball.domain.PlayerData}. Like {@link TeamOneLiner} it keys
 * on everything except its own text — here that is the language alone, since the player
 * sentence has one voice (plan §3) — so a regeneration replaces rather than duplicates.
 * <p>
 * There is no TTL. The snapshot fields are what make staleness detectable: the sentence is good
 * until the player has played, contributed, changed injury status or moved in the scoring
 * charts since it was written (plan §5).
 */
@Getter
@Builder
@AllArgsConstructor
public class PlayerOneLiner {

    Language language;

    String text;

    Instant generatedAt;

    @JsonIgnore
    Integer matchesPlayedAtGeneration;

    @JsonIgnore
    Integer goalsAtGeneration;

    @JsonIgnore
    Integer assistsAtGeneration;

    @JsonIgnore
    Boolean injuredAtGeneration;

    @JsonIgnore
    Integer scorerRankAtGeneration;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return language == ((PlayerOneLiner) obj).language;
    }

    @Override
    public int hashCode() {
        return Objects.hash(language);
    }
}
