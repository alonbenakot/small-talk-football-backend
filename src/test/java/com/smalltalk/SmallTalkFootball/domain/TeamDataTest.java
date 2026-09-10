package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Competition;
import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.Perspective;
import com.smalltalk.SmallTalkFootball.models.TeamOneLiner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The one-liner cache on TeamData. Two things here are load-bearing and easy to lose:
 * {@code @Builder.Default} on the Set (a Lombok builder ignores a field initializer, so
 * without it every builder-made team NPEs on the first add), and replaceOneLiner, since
 * TeamOneLiner equality ignores its text and add() alone would silently keep the old one.
 */
class TeamDataTest {

    private static TeamData team() {
        return TeamData.builder().id("2621").name("Liverpool").build();
    }

    private static TeamOneLiner oneLiner(String text) {
        return TeamOneLiner.builder()
                .language(Language.BRITISH)
                .competition(Competition.PREMIER_LEAGUE)
                .perspective(Perspective.FAN)
                .text(text)
                .build();
    }

    @Test
    void aBuilderMadeTeamStartsWithAnEmptyCacheRatherThanNull() {
        assertThatCode(() -> team().addOneLiner(oneLiner("first"))).doesNotThrowAnyException();
    }

    @Test
    void aTeamWithANullCacheStillReadsAsEmpty() {
        TeamData team = team();
        team.setOneLiners(null);

        assertThat(team.getOneLiners()).isEmpty();
        assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN)).isEmpty();
    }

    @Test
    void addingLeavesAnExistingEntryUntouched() {
        TeamData team = team();
        team.addOneLiner(oneLiner("first"));

        assertThat(team.addOneLiner(oneLiner("second"))).isFalse();
        assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN))
                .get().extracting(TeamOneLiner::getText).isEqualTo("first");
    }

    @Test
    void replacingOverwritesTheStoredText() {
        TeamData team = team();
        team.addOneLiner(oneLiner("first"));
        team.replaceOneLiner(oneLiner("second"));

        assertThat(team.getOneLiners()).hasSize(1);
        assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.FAN))
                .get().extracting(TeamOneLiner::getText).isEqualTo("second");
    }

    @Test
    void theExposedCacheCannotBeModifiedFromOutside() {
        TeamData team = team();
        team.addOneLiner(oneLiner("first"));

        assertThat(team.getOneLiners()).isUnmodifiable();
    }

    @Test
    void findsNothingForACombinationThatWasNeverGenerated() {
        TeamData team = team();
        team.addOneLiner(oneLiner("first"));

        assertThat(team.findOneLiner(Language.HEBREW, Competition.PREMIER_LEAGUE, Perspective.FAN)).isEmpty();
        assertThat(team.findOneLiner(Language.BRITISH, Competition.CHAMPIONS_LEAGUE, Perspective.FAN)).isEmpty();
        assertThat(team.findOneLiner(Language.BRITISH, Competition.PREMIER_LEAGUE, Perspective.NEUTRAL)).isEmpty();
    }
}
