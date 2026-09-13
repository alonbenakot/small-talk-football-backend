package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.models.PlayerOneLiner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The one-liner cache on PlayerData, mirroring TeamDataTest. {@code @Builder.Default} on the
 * Set and replaceOneLiner are both load-bearing: a Lombok builder ignores a field initializer,
 * and PlayerOneLiner equality ignores its text so add() alone would keep the stale sentence.
 */
class PlayerDataTest {

    private static PlayerData player() {
        return PlayerData.builder().id("p-1").teamId("80").name("Erling Haaland").build();
    }

    private static PlayerOneLiner oneLiner(String text) {
        return PlayerOneLiner.builder().language(Language.BRITISH).text(text).build();
    }

    @Test
    void aBuilderMadePlayerStartsWithAnEmptyCacheRatherThanNull() {
        assertThatCode(() -> player().addOneLiner(oneLiner("first"))).doesNotThrowAnyException();
    }

    @Test
    void aPlayerWithANullCacheStillReadsAsEmpty() {
        PlayerData player = player();
        player.setOneLiners(null);

        assertThat(player.getOneLiners()).isEmpty();
        assertThat(player.findOneLiner(Language.BRITISH)).isEmpty();
    }

    @Test
    void addingLeavesAnExistingEntryUntouched() {
        PlayerData player = player();
        player.addOneLiner(oneLiner("first"));

        assertThat(player.addOneLiner(oneLiner("second"))).isFalse();
        assertThat(player.findOneLiner(Language.BRITISH)).get().extracting(PlayerOneLiner::getText).isEqualTo("first");
    }

    @Test
    void replacingOverwritesTheStoredText() {
        PlayerData player = player();
        player.addOneLiner(oneLiner("first"));
        player.replaceOneLiner(oneLiner("second"));

        assertThat(player.getOneLiners()).hasSize(1);
        assertThat(player.findOneLiner(Language.BRITISH)).get().extracting(PlayerOneLiner::getText).isEqualTo("second");
    }

    @Test
    void theExposedCacheCannotBeModifiedFromOutside() {
        PlayerData player = player();
        player.addOneLiner(oneLiner("first"));

        assertThat(player.getOneLiners()).isUnmodifiable();
    }

    @Test
    void findsNothingForALanguageThatWasNeverGenerated() {
        PlayerData player = player();
        player.addOneLiner(oneLiner("first"));

        assertThat(player.findOneLiner(Language.HEBREW)).isEmpty();
    }
}
