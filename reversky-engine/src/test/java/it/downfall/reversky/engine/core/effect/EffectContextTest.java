package it.downfall.reversky.engine.core.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link EffectContext}: proprietario e avversario dipendono dal lato di chi possiede la carta.
 */
class EffectContextTest {

	private static final CardDefinition VEIL = TestCards.spell(96, "Veil of the Anchored", CardSubtype.SHIELD,
			"VEIL_OF_ANCHORED");

	private final MatchState state = TestStates.withDecks(List.of(), List.of());

	@Test
	@DisplayName("Carta dell'avversario: owner() è l'avversario, opponent() è lo sfidante; log e carta della partita")
	void opponentOwner_sidesSwapped() {
		// Act
		EffectContext ctx = new EffectContext(state, Side.OPPONENT, VEIL);

		// Assert
		assertThat(ctx.owner()).isSameAs(state.opponent());
		assertThat(ctx.opponent()).isSameAs(state.challenger());
		assertThat(ctx.log()).isSameAs(state.log());
		assertThat(ctx.state()).isSameAs(state);
		assertThat(ctx.card()).isEqualTo(VEIL);
	}

	@Test
	@DisplayName("Carta dello sfidante: owner() è lo sfidante")
	void challengerOwner() {
		assertThat(new EffectContext(state, Side.CHALLENGER, VEIL).owner()).isSameAs(state.challenger());
	}

	@Test
	@DisplayName("Partita, lato e carta sono obbligatori")
	void missingArgument_throws() {
		assertThatThrownBy(() -> new EffectContext(null, Side.CHALLENGER, VEIL))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EffectContext(state, null, VEIL)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new EffectContext(state, Side.CHALLENGER, null))
				.isInstanceOf(NullPointerException.class);
	}
}
