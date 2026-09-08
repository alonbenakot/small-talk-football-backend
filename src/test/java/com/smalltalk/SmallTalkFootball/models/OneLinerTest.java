package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OneLiner deliberately excludes its text from equals/hashCode, so a Set of one-liners is
 * keyed by team and language. That is what makes Fixture.oneLiners behave as a per-team,
 * per-language cache, and OneLinersService depends on it.
 */
class OneLinerTest {

    private static OneLiner oneLiner(TeamType teamType, Language language, String text) {
        return OneLiner.builder().teamType(teamType).language(language).text(text).build();
    }

    @Test
    void oneLinersWithTheSameTeamAndLanguageAreEqualRegardlessOfText() {
        OneLiner first = oneLiner(TeamType.HOME, Language.BRITISH, "What a win for the Reds.");
        OneLiner second = oneLiner(TeamType.HOME, Language.BRITISH, "Completely different text.");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void differentTeamsAreNotEqual() {
        assertThat(oneLiner(TeamType.HOME, Language.BRITISH, "same"))
                .isNotEqualTo(oneLiner(TeamType.AWAY, Language.BRITISH, "same"));
    }

    @Test
    void differentLanguagesAreNotEqual() {
        assertThat(oneLiner(TeamType.HOME, Language.BRITISH, "same"))
                .isNotEqualTo(oneLiner(TeamType.HOME, Language.AMERICAN, "same"));
    }

    @Test
    void nullTeamTypeIsItsOwnCacheKey() {
        OneLiner neutral = oneLiner(null, Language.BRITISH, "A neutral take.");

        assertThat(neutral).isEqualTo(oneLiner(null, Language.BRITISH, "Another neutral take."));
        assertThat(neutral).isNotEqualTo(oneLiner(TeamType.HOME, Language.BRITISH, "A neutral take."));
    }

    @Test
    void aSetHoldsOneEntryPerTeamAndLanguageCombination() {
        Set<OneLiner> oneLiners = new HashSet<>();

        oneLiners.add(oneLiner(TeamType.HOME, Language.BRITISH, "first"));
        oneLiners.add(oneLiner(TeamType.HOME, Language.BRITISH, "second"));
        oneLiners.add(oneLiner(TeamType.AWAY, Language.BRITISH, "third"));
        oneLiners.add(oneLiner(TeamType.HOME, Language.HEBREW, "fourth"));

        assertThat(oneLiners).hasSize(3);
    }

    @Test
    void isNotEqualToOtherTypesOrNull() {
        OneLiner oneLiner = oneLiner(TeamType.HOME, Language.BRITISH, "text");

        assertThat(oneLiner).isNotEqualTo(null);
        assertThat(oneLiner).isNotEqualTo("not a one-liner");
        assertThat(oneLiner).isEqualTo(oneLiner);
    }
}
