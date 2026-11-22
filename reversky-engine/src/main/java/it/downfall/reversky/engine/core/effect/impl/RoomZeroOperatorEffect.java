package it.downfall.reversky.engine.core.effect.impl;

import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.log.MatchLog;

/**
 * L'Operatore della Stanza 0, Rare (Theory of the Wound), <i>Linee incrociate</i>: "all'entrata, la prossima Hidden
 * avversaria che si attiva funziona come se fosse del proprietario dell'Operatore".
 * <p>
 * Mette un incrocio sulla zona Hidden avversaria (M-11, ENG-39, deciso il 19 novembre): la prossima Hidden che si
 * attiva da quella zona, in qualunque momento della partita e anche dopo la morte dell'Operatore, lavora per il suo
 * proprietario (una {@code DAMAGE} colpisce la creatura del suo proprietario, una {@code SURVIVAL} aiuta quella
 * dell'Operatore, una con condizione si controlla sulla creatura dell'Operatore). Gli incroci si sommano. Passa da
 * {@link CreatureContext#targetEnemy()}: Il Custode delle Porte Incrociate ne è immune e il Fascicolo Sigillato 0
 * dell'avversario lo ferma.
 */
public final class RoomZeroOperatorEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "ROOM_ZERO_OPERATOR";

	/** Crea l'effetto. */
	public RoomZeroOperatorEffect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		if (ctx.targetEnemy().isEmpty()) {
			return;
		}
		ctx.opponent().crossNextHidden(ctx.self().name());
		EffectActions.log(ctx, List.of("crosses the lines: the next " + MatchLog.player(ctx.opponent().side())
				+ " Hidden works for the " + MatchLog.player(ctx.self().side())));
	}
}
