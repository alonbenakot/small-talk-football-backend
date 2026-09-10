package com.smalltalk.SmallTalkFootball.models;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TeamOneLiner keys on language, competition and perspective while ignoring its own text, so
 * the Set on TeamData holds exactly one sentence per combination. TeamOneLinersService relies
 * on that — and on replaceOneLiner rather than add, since add would be a no-op here.
 */
class TeamOneLinerTest {

    private static TeamOneLiner oneLiner(Language language, Competition competition,
                                         Perspective perspective, String text) {
        return TeamOneLiner.builder()
                .language(language)
                .competition(competition)
                .perspective(perspective)
                .text(text)
                .generatedAt(Instant.parse("2026-09-09T10:00:00Z"))
                .build();
    }

    @Test
    void sameLanguageCompetitionAndPerspectiveAreEqualRegardlessOfText() {
        TeamOneLiner first = oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "Top of the table.");
        TeamOneLiner second = oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "Something else.");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void differentPerspectivesAreNotEqual() {
        assertThat(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "same"))
                .isNotEqualTo(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.RIVAL_FAN, "same"));
    }

    @Test
    void differentCompetitionsAreNotEqual() {
        assertThat(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "same"))
                .isNotEqualTo(oneLiner(Language.BRITISH, Competition.CHAMPIONS_LEAGUE, Perspective.FAN, "same"));
    }

    @Test
    void differentLanguagesAreNotEqual() {
        assertThat(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "same"))
                .isNotEqualTo(oneLiner(Language.HEBREW, Competition.PREMIER_LEAGUE, Perspective.FAN, "same"));
    }

    @Test
    void aSetHoldsOneEntryPerLanguageCompetitionAndPerspective() {
        Set<TeamOneLiner> oneLiners = new HashSet<>();

        oneLiners.add(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "first"));
        oneLiners.add(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "second"));
        oneLiners.add(oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.RIVAL_FAN, "third"));
        oneLiners.add(oneLiner(Language.HEBREW, Competition.PREMIER_LEAGUE, Perspective.FAN, "fourth"));
        oneLiners.add(oneLiner(Language.BRITISH, Competition.CHAMPIONS_LEAGUE, Perspective.FAN, "fifth"));

        assertThat(oneLiners).hasSize(4);
    }

    @Test
    void isNotEqualToOtherTypesOrNull() {
        TeamOneLiner oneLiner = oneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN, "text");

        assertThat(oneLiner).isNotEqualTo(null);
        assertThat(oneLiner).isNotEqualTo("not a one-liner");
        assertThat(oneLiner).isEqualTo(oneLiner);
    }
}
