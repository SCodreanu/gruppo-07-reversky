package it.downfall.reversky.engine.core.effect.impl;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;

/**
 * SCP-500 · Pillola di Emergenza, Spell {@code SHIELD}: "la prossima creatura subisce 5 danni in meno dal primo danno
 * di qualunque tipo (attacco o effetto)".
 * <p>
 * Una protezione in attesa della prossima creatura del proprietario (D-34, ENG-40, deciso il 20 novembre), applicata
 * all'entrata prima degli altri modificatori in attesa, così ferma anche un danno avversario in attesa. Il primo danno
 * sopra 0 la usa tutta (D-33): un attacco prima degli effetti della creatura colpita (quindi prima del dimezzamento di
 * SCP-3000 e di Unseen Exit), un danno da effetto dopo il Terreno in gioco. Due SCP-500 si sommano sullo stesso primo
 * danno. È l'effetto di una Spell, non della creatura e non un modificatore: annullamenti, effetti divorati e
 * rimozioni di bonus non la toccano.
 */
public final class Scp500Effect implements SpellEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "SCP_500";

	private static final int WARD = 5;

	/** Crea l'effetto. */
	public Scp500Effect() {
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void resolve(EffectContext ctx) {
		PendingModifier pill = PendingModifier.firstDamageWard(ctx.card().name(), ModifierSource.SPELL,
				ctx.owner().side(), WARD);
		ctx.owner().addPendingModifier(pill);
		ctx.log().spellResolved(ctx.owner().side(), ctx.card(), MatchLog.describe(pill), null);
	}
}
