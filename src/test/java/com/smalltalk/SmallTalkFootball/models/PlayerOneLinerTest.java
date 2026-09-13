package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Language;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlayerOneLiner keys on language alone while ignoring its own text, so the Set on PlayerData
 * holds exactly one sentence per language. PlayerOneLinersService relies on that — and on
 * replaceOneLiner rather than add, since add would be a no-op here.
 */
class PlayerOneLinerTest {

    private static PlayerOneLiner oneLiner(Language language, String text) {
        return PlayerOneLiner.builder()
                .language(language)
                .text(text)
                .generatedAt(Instant.parse("2026-09-09T10:00:00Z"))
                .build();
    }

    @Test
    void sameLanguageIsEqualRegardlessOfText() {
        PlayerOneLiner first = oneLiner(Language.BRITISH, "Eight in ten.");
        PlayerOneLiner second = oneLiner(Language.BRITISH, "Something else.");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void differentLanguagesAreNotEqual() {
        assertThat(oneLiner(Language.BRITISH, "same")).isNotEqualTo(oneLiner(Language.HEBREW, "same"));
    }

    /** The snapshot is a caching detail; two sentences written against different stats still collide. */
    @Test
    void theSnapshotFieldsDoNotTakePartInEquality() {
        PlayerOneLiner first = PlayerOneLiner.builder().language(Language.BRITISH).goalsAtGeneration(3).build();
        PlayerOneLiner second = PlayerOneLiner.builder().language(Language.BRITISH).goalsAtGeneration(8).build();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void aSetHoldsOneEntryPerLanguage() {
        Set<PlayerOneLiner> oneLiners = new HashSet<>();

        oneLiners.add(oneLiner(Language.BRITISH, "first"));
        oneLiners.add(oneLiner(Language.BRITISH, "second"));
        oneLiners.add(oneLiner(Language.HEBREW, "third"));

        assertThat(oneLiners).hasSize(2);
    }

    @Test
    void isNotEqualToOtherTypesOrNull() {
        PlayerOneLiner oneLiner = oneLiner(Language.BRITISH, "text");

        assertThat(oneLiner).isNotEqualTo(null);
        assertThat(oneLiner).isNotEqualTo("not a one-liner");
        assertThat(oneLiner).isEqualTo(oneLiner);
    }
}
