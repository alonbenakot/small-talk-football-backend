package com.smalltalk.SmallTalkFootball.domain;

import com.smalltalk.SmallTalkFootball.enums.Language;
import com.smalltalk.SmallTalkFootball.enums.TeamType;
import com.smalltalk.SmallTalkFootball.models.OneLiner;
import com.smalltalk.SmallTalkFootball.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureTest {

    private static OneLiner oneLiner(TeamType teamType, Language language, String text) {
        return OneLiner.builder().teamType(teamType).language(language).text(text).build();
    }

    @Test
    void exposesOneLinersAsAnUnmodifiableView() {
        Fixture fixture = TestFixtures.finishedFixture().build();
        fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "A win is a win."));

        assertThatThrownBy(() -> fixture.getOneLiners().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void keepsTheFirstOneLinerForATeamAndLanguage() {
        Fixture fixture = TestFixtures.finishedFixture().build();

        assertThat(fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "original"))).isTrue();
        assertThat(fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "replacement"))).isFalse();

        assertThat(fixture.getOneLiners()).hasSize(1);
        assertThat(fixture.getOneLiners().iterator().next().getText()).isEqualTo("original");
    }

    @Test
    void replaceSwapsTheTextForATeamAndLanguage() {
        Fixture fixture = TestFixtures.finishedFixture().build();
        fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "original"));

        fixture.replaceOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "replacement"));

        assertThat(fixture.getOneLiners()).hasSize(1);
        assertThat(fixture.getOneLiners().iterator().next().getText()).isEqualTo("replacement");
    }

    @Test
    void storesOneLinersPerTeamAndLanguage() {
        Fixture fixture = TestFixtures.finishedFixture().build();

        fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "home british"));
        fixture.addOneLiner(oneLiner(TeamType.AWAY, Language.BRITISH, "away british"));
        fixture.addOneLiner(oneLiner(TeamType.HOME, Language.HEBREW, "home hebrew"));

        assertThat(fixture.getOneLiners()).hasSize(3);
    }

    @Test
    void readingOneLinersIsSafeWhenTheSetWasNeverInitialised() {
        Fixture fixture = new Fixture();
        fixture.setOneLiners(null);

        assertThat(fixture.getOneLiners()).isEmpty();
    }

    /*
     * Pins a Lombok trap. Fixture declares `oneLiners = new HashSet<>()`, but @Builder ignores
     * field initialisers unless they are marked @Builder.Default, so a builder-made Fixture -
     * which is how FixtureAssembler makes every one - starts with a null set. Reading is safe
     * because getOneLiners() guards for null, but adding is not.
     *
     * OneLinersService only calls addOneLiner on fixtures loaded from Mongo, where Spring Data
     * uses the no-arg constructor and the initialiser does run, so this is latent rather than
     * live. If oneLiners ever gains @Builder.Default this test should be updated.
     */
    @Test
    void builderLeavesTheOneLinerSetNullSoAddingThrows() {
        Fixture fixture = Fixture.builder().externalId(1).build();

        assertThat(fixture.getOneLiners()).isEmpty();
        assertThatThrownBy(() -> fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "boom")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void noArgConstructorStartsWithAnEmptyMutableSet() {
        Fixture fixture = new Fixture();

        assertThat(fixture.addOneLiner(oneLiner(TeamType.HOME, Language.BRITISH, "fine"))).isTrue();
        assertThat(fixture.getOneLiners()).hasSize(1);
    }
}
