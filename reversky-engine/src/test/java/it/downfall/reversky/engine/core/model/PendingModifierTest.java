package it.downfall.reversky.engine.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestFactions;

/**
 * Test di {@link PendingModifier}: cosa fa a una creatura quando la raggiunge (D-34, D-12, D-35, D-49) e come appare
 * nel log.
 */
class PendingModifierTest {

	private final CreatureInPlay kappa = TestCards.inPlay(TestCards.creature("Kappa", TestFactions.DREAM, 6, 7),
			Side.OPPONENT);

	@Test
	@DisplayName("ATK e DEF in più: un modificatore ATK e uno scudo finché la creatura resta in campo, con l'origine")
	void applyTo_bonus_attackModifierAndShield() {
		// Act
		new PendingModifier("Registry Pact", ModifierSource.SPELL, Side.OPPONENT, 1, 2, 0).applyTo(kappa);

		// Assert
		assertThat(kappa.attack()).isEqualTo(7);
		assertThat(kappa.defense()).isEqualTo(9);
		assertThat(kappa.modifiers()).allSatisfy(modifier -> {
			assertThat(modifier.source()).isEqualTo(ModifierSource.SPELL);
			assertThat(modifier.durationKind()).isEqualTo(ModifierDuration.Kind.WHILE_IN_PLAY);
		});
		kappa.endCombat();
		assertThat(kappa.defense()).isEqualTo(9);
	}

	@Test
	@DisplayName("Malus e danno da effetto: −2 ATK e −2 DEF permanente (D-49)")
	void applyTo_penalty_attackMalusAndPermanentDamage() {
		// Act
		new PendingModifier("Dissolution", ModifierSource.SPELL, Side.CHALLENGER, -2, 0, 2).applyTo(kappa);

		// Assert
		assertThat(kappa.attack()).isEqualTo(4);
		assertThat(kappa.defense()).isEqualTo(5);
		assertThat(kappa.modifiers()).singleElement()
				.satisfies(modifier -> assertThat(modifier.side()).isEqualTo(Side.CHALLENGER));
	}

	@Test
	@DisplayName("Il danno da effetto può uccidere: DEF 7 − 9 = −2")
	void applyTo_lethalDamage_dead() {
		// Act
		new PendingModifier("Skin Fracture", ModifierSource.SPELL, Side.CHALLENGER, 0, 0, 9).applyTo(kappa);

		// Assert
		assertThat(kappa.isDead()).isTrue();
		assertThat(kappa.defense()).isEqualTo(-2);
	}

	@Test
	@DisplayName("Nel log: +1 ATK, +2 DEF; −3 ATK; −5 DEF con il segno meno tipografico")
	void describe_signedParts() {
		assertThat(MatchLog.describe(new PendingModifier("Pact", ModifierSource.SPELL, Side.CHALLENGER, 1, 2, 0)))
				.isEqualTo("+1 ATK, +2 DEF");
		assertThat(MatchLog.describe(new PendingModifier("Bite", ModifierSource.SPELL, Side.CHALLENGER, -3, 0, 0)))
				.isEqualTo("−3 ATK");
		assertThat(MatchLog.describe(new PendingModifier("Skin", ModifierSource.SPELL, Side.CHALLENGER, 0, 0, 5)))
				.isEqualTo("−5 DEF");
	}

	@Test
	@DisplayName("Scudo o danno negativi, o nessun cambiamento: non è un modificatore valido")
	void invalidValues_throw() {
		assertThatThrownBy(() -> new PendingModifier("X", ModifierSource.SPELL, Side.CHALLENGER, 0, -1, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PendingModifier("X", ModifierSource.SPELL, Side.CHALLENGER, 0, 0, -1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PendingModifier("X", ModifierSource.SPELL, Side.CHALLENGER, 0, 0, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PendingModifier(null, ModifierSource.SPELL, Side.CHALLENGER, 1, 0, 0))
				.isInstanceOf(NullPointerException.class);
	}
}
