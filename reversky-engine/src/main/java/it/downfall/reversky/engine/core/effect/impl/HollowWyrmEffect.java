package it.downfall.reversky.engine.core.effect.impl;

import java.util.ArrayList;
import java.util.List;

import it.downfall.reversky.engine.core.effect.CreatureContext;
import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.Terrains;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.ModifierDuration;

/**
 * The Hollow Wyrm, Legendary (Theory of the Wound), <i>Divoratore di Crepe</i>: "all'entrata inghiotte il Terreno in
 * gioco, chiunque l'abbia pescato (scartato) e ottiene +3 ATK e +3 DEF finché resta in campo; un Terreno pescato
 * mentre è in campo viene inghiottito allo stesso modo; senza Terreno all'entrata, 4 danni all'avv.".
 * <p>
 * Il Terreno inghiottito esce con i suoi bonus (D-19), anche quello della sua fazione; uno pescato mentre il Wyrm è in
 * campo non si piazza ({@code onTerrainArriving}, ENG-36). Ogni Terreno inghiottito dà +3/+3: l'ATK è un bonus con
 * origine {@code EFFECT}, la DEF è permanente (docs/bilanciamento/revisione-effetti: "il bonus ATK è sospeso, la DEF
 * resta"). Inghiottire non colpisce la creatura avversaria, quindi il Fascicolo Sigillato 0 ferma solo i 4 danni.
 */
public final class HollowWyrmEffect implements CreatureEffect {

	/** Codice dell'effetto ({@code catalog_cards.effect_code}). */
	public static final String CODE = "HOLLOW_WYRM";

	private static final int ATTACK_GAIN = 3;
	private static final int DEFENSE_GAIN = 3;
	private static final int NO_TERRAIN_DAMAGE = 4;

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public void onEnterPlay(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		if (Terrains.remove(ctx.state(), self.side(), self.name()).isPresent()) {
			grow(ctx);
			return;
		}
		List<String> parts = new ArrayList<>();
		ctx.targetEnemy().ifPresent(enemy -> parts.add(EffectActions.damage(ctx, enemy, NO_TERRAIN_DAMAGE)));
		EffectActions.log(ctx, parts);
	}

	@Override
	public boolean onTerrainArriving(CreatureContext ctx, CardDefinition terrain) {
		ctx.log().terrainRemoved(ctx.self().side(), terrain, ctx.self().name());
		grow(ctx);
		return true;
	}

	private static void grow(CreatureContext ctx) {
		CreatureInPlay self = ctx.self();
		EffectActions.log(ctx, List.of(
				EffectActions.attack(self, self, ATTACK_GAIN, ModifierDuration.whileInPlay(), ""),
				EffectActions.defense(self, DEFENSE_GAIN)));
	}
}
