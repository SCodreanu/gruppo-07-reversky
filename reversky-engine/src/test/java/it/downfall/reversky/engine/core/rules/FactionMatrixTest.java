package it.downfall.reversky.engine.core.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test di {@link FactionMatrix} con i bonus di D-04 (docs/fazioni, "Valori"): +2 ATK contro la fazione che si
 * batte, −1 ATK contro quella che ti batte, 0 contro quella neutra.
 * <p>
 * Che l'attacco effettivo con il malus non scenda sotto 0 si prova con {@code RoundResolver} (ENG-06): la
 * matrice restituisce il bonus così com'è, anche negativo.
 */
class FactionMatrixTest {

	private static final int WOUND = 1;        // batte Dream
	private static final int INVITATION = 2;   // batte Archive
	private static final int ARCHIVE = 3;      // batte Wound
	private static final int DREAM = 4;        // batte Invitation

	private static final List<Integer> ONLY_WOUND = List.of(WOUND);
	private static final List<Integer> ONLY_ARCHIVE = List.of(ARCHIVE);
	private static final List<Integer> ONLY_DREAM = List.of(DREAM);
	/** The Rootbound Doppelganger: Dream + Invitation (esempi 11 e 12). */
	private static final List<Integer> ROOTBOUND = List.of(DREAM, INVITATION);
	/** The Stitched Corridor: Wound + Archive. */
	private static final List<Integer> STITCHED = List.of(WOUND, ARCHIVE);

	private final FactionMatrix matrix = new FactionMatrix(List.of(
			new FactionBonus(WOUND, DREAM, 2, 0),
			new FactionBonus(INVITATION, ARCHIVE, 2, 0),
			new FactionBonus(ARCHIVE, WOUND, 2, 0),
			new FactionBonus(DREAM, INVITATION, 2, 0),
			new FactionBonus(DREAM, WOUND, -1, 0),
			new FactionBonus(ARCHIVE, INVITATION, -1, 0),
			new FactionBonus(WOUND, ARCHIVE, -1, 0),
			new FactionBonus(INVITATION, DREAM, -1, 0)));

	@Test
	@DisplayName("Una coppia con una riga restituisce il bonus della riga: Wound contro Dream +2")
	void atkBonus_pairWithRow_returnsRowBonus() {
		assertThat(matrix.atkBonus(ONLY_WOUND, ONLY_DREAM)).isEqualTo(2);
	}

	@Test
	@DisplayName("Il malus resta negativo: Dream contro Wound −1")
	void atkBonus_malusRow_returnsNegativeBonus() {
		assertThat(matrix.atkBonus(ONLY_DREAM, ONLY_WOUND)).isEqualTo(-1);
	}

	@Test
	@DisplayName("Una coppia senza riga non ha bonus: Wound contro Invitation (neutra) 0")
	void atkBonus_pairWithoutRow_returnsZero() {
		assertThat(matrix.atkBonus(ONLY_WOUND, List.of(INVITATION))).isZero();
	}

	@Test
	@DisplayName("Una fazione contro se stessa non ha bonus: Dream contro Dream 0")
	void atkBonus_sameFaction_returnsZero() {
		assertThat(matrix.atkBonus(ONLY_DREAM, ONLY_DREAM)).isZero();
	}

	@Test
	@DisplayName("Senza righe di bonus ogni coppia vale 0")
	void atkBonus_noRows_returnsZero() {
		assertThat(new FactionMatrix(List.of()).atkBonus(ONLY_WOUND, ONLY_DREAM)).isZero();
	}

	@Test
	@DisplayName("Una carta senza fazione (lista vuota) non ha coppie: bonus 0 in attacco e in difesa")
	void atkBonus_noFactions_returnsZero() {
		assertThat(matrix.atkBonus(List.of(), ONLY_DREAM)).isZero();
		assertThat(matrix.atkBonus(ONLY_WOUND, List.of())).isZero();
	}

	@Test
	@DisplayName("Due fazioni in attacco (es. 11): Rootbound contro Archive = Dream→Archive 0 + Inv.→Archive +2 = +2")
	void atkBonus_twoFactionAttacker_sumsAllPairs() {
		assertThat(matrix.atkBonus(ROOTBOUND, ONLY_ARCHIVE)).isEqualTo(2);
	}

	@Test
	@DisplayName("Due fazioni in difesa (es. 11): Archive contro Rootbound = Archive→Dream 0 + Archive→Inv. −1 = −1")
	void atkBonus_twoFactionDefender_sumsAllPairs() {
		assertThat(matrix.atkBonus(ONLY_ARCHIVE, ROOTBOUND)).isEqualTo(-1);
	}

	@Test
	@DisplayName("Due fazioni con svantaggio (esempio 12): Rootbound contro Dream −1, Dream contro Rootbound +2")
	void atkBonus_twoFactionCardAgainstOwnFaction_sumsBothPairs() {
		assertThat(matrix.atkBonus(ROOTBOUND, ONLY_DREAM)).isEqualTo(-1);
		assertThat(matrix.atkBonus(ONLY_DREAM, ROOTBOUND)).isEqualTo(2);
	}

	@Test
	@DisplayName("Due carte con due fazioni, quattro coppie: Rootbound→Stitched −1+0+0+2 = +1, inverso +2+0+0−1 = +1")
	void atkBonus_twoTwoFactionCards_sumsFourPairs() {
		assertThat(matrix.atkBonus(ROOTBOUND, STITCHED)).isEqualTo(1);
		assertThat(matrix.atkBonus(STITCHED, ROOTBOUND)).isEqualTo(1);
	}

	@Test
	@DisplayName("defBonus usa la stessa riga: Wound contro Dream con defBonus 3 → 3; la direzione opposta → 0")
	void defBonus_pairWithRow_returnsRowDefBonus() {
		// Arrange
		FactionMatrix withDefence = new FactionMatrix(List.of(new FactionBonus(WOUND, DREAM, 0, 3)));

		// Act + Assert
		assertThat(withDefence.defBonus(ONLY_WOUND, ONLY_DREAM)).isEqualTo(3);
		assertThat(withDefence.defBonus(ONLY_DREAM, ONLY_WOUND)).isZero();
	}

	@Test
	@DisplayName("defBonus somma tutte le coppie: Stitched contro Dream = Wound→Dream 3 + Archive→Dream 1 = 4")
	void defBonus_twoFactionCard_sumsAllPairs() {
		// Arrange
		FactionMatrix withDefence = new FactionMatrix(List.of(
				new FactionBonus(WOUND, DREAM, 0, 3),
				new FactionBonus(ARCHIVE, DREAM, 0, 1)));

		// Act + Assert
		assertThat(withDefence.defBonus(STITCHED, ONLY_DREAM)).isEqualTo(4);
	}
}
