package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.PlayerState;

/**
 * La Regina dei Sentieri Perduti, Legendary (Theory of the Invitation), <i>Sentieri deviati</i>: "all'entrata e a ogni
 * eliminazione guarda le prossime tre creature del mazzo avversario e manda quella di rarità più alta in fondo al
 * mazzo (a parità, quella che verrebbe pescata prima); all'entrata anche avv. −2 DEF".
 * <p>
 * Si contano solo le creature; Spell, Hidden, Terreni e Oggetti restano al loro posto, e la carta scelta va in fondo
 * a tutto il mazzo. Rarità confrontata con {@code rarityOrder} (Mutant sopra Legendary, D-20). Con meno di due
 * creature nel mazzo avversario non succede nulla; se la carta scelta è già l'ultima del mazzo non c'è
 * {@code DECK_REORDERED} (ENG-39, deciso il 19 novembre). Lo spostamento non colpisce la creatura avversaria, quindi
 * Il Custode delle Porte Incrociate e il Fascicolo Sigillato 0 non lo fermano; il −2 DEF, che viene dopo, sì.
 * "A ogni eliminazione" è {@code onKill} (D-42).
 */
public final class QueenOfLostPathsEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "QUEEN_OF_LOST_PATHS";

	private static final int LOOKED_AT = 3;
	private static final int MIN_CREATURES = 2;
	private static final int DAMAGE = 2;

	/** Crea l'effetto. */
	public QueenOfLostPathsEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		reorder(ctx);
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, DAMAGE)));
		EffectActions.log(ctx, parts);
	}

	@Override
	public void onKill(CreatureContext ctx, CreatureInPlay defeated) {
		reorder(ctx);
	}

	private static void reorder(CreatureContext ctx) {
		PlayerState opponent = ctx.opponent();
		List<CardDefinition> deck = opponent.deck();
		if (deck.stream().filter(card -> card.type() == CardType.CREATURE).count() < MIN_CREATURES) {
			return;
		}
		int chosen = -1;
		int looked = 0;
		for (int index = 0; index < deck.size() && looked < LOOKED_AT; index++) {
			CardDefinition card = deck.get(index);
			if (card.type() != CardType.CREATURE) {
				continue;
			}
			looked++;
			// a parità resta quella che verrebbe pescata prima
			if (chosen < 0 || card.rarityOrder() > deck.get(chosen).rarityOrder()) {
				chosen = index;
			}
		}
		if (chosen == deck.size() - 1) {
			return;
		}
		int position = opponent.moveToBottom(chosen);
		ctx.log().deckReordered(opponent.side(), ctx.self().name(), deck.get(chosen), position);
	}
}
