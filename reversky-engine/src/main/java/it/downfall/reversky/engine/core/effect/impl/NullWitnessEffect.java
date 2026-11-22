package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.Terrains;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;

/**
 * The Null Witness, Mythic (Theory of the Archive), <i>Protocollo di cancellazione</i>: "all'entrata 4 danni all'avv.,
 * che perde i suoi modificatori temporanei; il Terreno in gioco viene rimosso se l'ha pescato l'avversario. Resta in
 * campo" (D-38).
 * <p>
 * Prima i 4 danni, che uno scudo temporaneo può ancora assorbire, poi l'avversaria perde tutti i modificatori con una
 * durata, di ogni segno ({@link CreatureInPlay#removeTemporaryModifiers()}, ENG-36, deciso il 18 novembre). Il
 * Fascicolo Sigillato 0 salva la creatura, non il Terreno: il Terreno pescato dall'avversario esce comunque, con i suoi
 * bonus (D-19).
 */
public final class NullWitnessEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "NULL_WITNESS";

	private static final int DAMAGE = 4;

	/** Crea l'effetto. */
	public NullWitnessEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> {
			parts.add(EffectActions.damage(ctx, enemy, DAMAGE));
			if (enemy.removeTemporaryModifiers()) {
				parts.add(enemy.name() + " loses its temporary modifiers (ATK " + MatchLog.number(enemy.attack())
						+ ", DEF " + MatchLog.number(enemy.defense()) + ")");
			}
		});
		EffectActions.log(ctx, parts);
		MatchState state = ctx.state();
		CreatureInPlay self = ctx.self();
		if (state.terrain().filter(terrain -> terrain.owner() != self.side()).isPresent()) {
			Terrains.remove(state, self.side(), self.name());
		}
	}
}
