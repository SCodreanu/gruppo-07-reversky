package it.downfall.reversky.engine.core.effect;

import java.util.Optional;

import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Danni che non sono attacchi (docs/effetti, "Danno da effetto"), con le riduzioni del Terreno in gioco (ENG-27,
 * deciso il 18 novembre).
 * <p>
 * Ogni perdita di DEF che non viene da un attacco passa da qui: effetti di creatura, Spell, Hidden, Terreni e
 * modificatori in attesa. Se viene dall'avversario è una <b>penalità</b>, e il Terreno in gioco la può ridurre per le
 * creature della sua fazione (Hollow Clearing), anche prima che ricevano il suo bonus. La prima Spell {@code DAMAGE}
 * di ogni scontro può essere ridotta a sua volta (Watching Rift). Gli attacchi e le perdite che una creatura si
 * infligge da sola (The Thorned Matriarch) non passano da qui.
 * <p>
 * Qui c'è anche la protezione dal primo danno (SCP-500, ENG-40, deciso il 20 novembre), che vale per il danno da
 * effetto, dopo il Terreno, e per gli attacchi, prima degli effetti della creatura colpita.
 */
public final class Penalties {

	private Penalties() {
	}

	/**
	 * Danno da effetto alla DEF (D-08, D-49). Se viene dall'avversario della creatura colpita, il Terreno in gioco può
	 * ridurlo; poi la creatura lo subisce, con la sua causa. Le morti le controlla il motore.
	 *
	 * @param state  partita
	 * @param target creatura colpita
	 * @param amount danno, almeno 0
	 * @param source lato che infligge il danno
	 * @param cause  creatura che lo infligge (effetto di creatura, Devouring Abyss), o {@code null} per Spell, Hidden e
	 *               modificatori in attesa
	 * @return il danno inflitto davvero, dopo le riduzioni
	 */
	public static int defense(MatchState state, CreatureInPlay target, int amount, Side source,
			CreatureInPlay cause) {
		int dealt = amount;
		if (source != target.side()) {
			Optional<PlacedTerrain> terrain = terrainFor(state, target);
			if (terrain.isPresent()) {
				dealt = terrain.get().effect().reduceDefensePenalty(state, terrain.get(), target, amount);
			}
		}
		// ENG-40: dopo il Terreno, così una penalità che il Terreno porta a 0 non usa la protezione
		dealt = ward(state, target, dealt);
		if (cause == null) {
			target.takeDamage(dealt);
		} else {
			target.takeDamage(dealt, cause);
		}
		if (source != target.side()) {
			// ENG-37: la DEF persa per una carta avversaria è una penalità (Remnants of Reality)
			target.recordDefensePenalty(dealt);
		}
		return dealt;
	}

	/**
	 * Applica un modificatore in attesa o il colpo di una Spell {@code DAMAGE} (D-34): ATK e scudo, poi il danno, che
	 * lo scudo appena dato assorbe per primo (D-35). Una Spell {@code DAMAGE} dell'avversario può perdere un punto per
	 * il Terreno in gioco (Watching Rift): prima dalla DEF, poi dall'ATK. Un effetto collegato passa alla creatura
	 * (SCP-109, ENG-34). Le morti le controlla il motore.
	 *
	 * @param state    partita
	 * @param target   creatura che lo riceve
	 * @param modifier il modificatore
	 */
	public static void apply(MatchState state, CreatureInPlay target, PendingModifier modifier) {
		int attack = modifier.attack();
		int damage = modifier.damage();
		boolean enemySpell = modifier.source() == ModifierSource.SPELL && modifier.side() != target.side();
		if (enemySpell && (damage > 0 || attack < 0)) {
			Optional<PlacedTerrain> terrain = terrainFor(state, target);
			if (terrain.isPresent()
					&& terrain.get().effect().reduceDamageSpell(state, terrain.get(), target, modifier.card()) > 0) {
				if (damage > 0) {
					damage--;
				} else {
					attack++;
				}
			}
		}
		if (attack != 0) {
			target.addModifier(StatModifier.attack(modifier.source(), modifier.side(), attack,
					ModifierDuration.whileInPlay()));
		}
		if (modifier.shield() > 0) {
			target.addModifier(StatModifier.shield(modifier.source(), modifier.side(), modifier.shield(),
					ModifierDuration.whileInPlay()));
		}
		if (modifier.attached() != null) {
			target.attachEffect(modifier.attached());
		}
		if (modifier.ward() > 0) {
			target.addDamageWard(modifier.card(), modifier.ward());
		}
		if (damage > 0) {
			defense(state, target, damage, modifier.side(), null);
		}
	}

	/**
	 * Riduce un danno con la protezione dal primo danno della creatura (SCP-500, ENG-40, deciso il 20 novembre), da
	 * chiunque venga: un danno sopra 0 la usa tutta e lo scrive nel log, un danno da 0 non la tocca.
	 *
	 * @param state  partita, per il log
	 * @param target creatura colpita
	 * @param damage danno in arrivo, almeno 0
	 * @return il danno che resta, almeno 0
	 */
	public static int ward(MatchState state, CreatureInPlay target, int damage) {
		if (damage <= 0 || target.damageWard() == 0) {
			return damage;
		}
		String cards = String.join(" and ", target.damageWardCards().stream().distinct().toList());
		int remaining = target.useDamageWard(damage);
		state.log().effectTriggered(target.side(), target.name(),
				"prevents " + (damage - remaining) + " of the first damage with " + cards);
		return remaining;
	}

	private static Optional<PlacedTerrain> terrainFor(MatchState state, CreatureInPlay target) {
		return state.terrain().filter(terrain -> terrain.affects(target));
	}
}
