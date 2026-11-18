package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;

/**
 * Pescata (regole-base, "Pescare"): un giocatore con il campo vuoto pesca finché esce una creatura, che entra in campo.
 * Le carte non creatura pescate prima si risolvono subito, nell'ordine in cui escono.
 * <ul>
 * <li>Spell: l'effetto si risolve e la carta si scarta (D-10). Una Spell {@code DAMAGE} può uccidere la creatura
 * avversaria: le morti si controllano subito dopo (D-08).</li>
 * <li>Hidden, Terreni e Oggetti: per ora si registrano e si scartano (ENG-20, ENG-21, ENG-33).</li>
 * </ul>
 * Se nel mazzo restano solo carte non creatura si risolvono tutte (D-06); la fine della partita la controlla il
 * simulatore.
 */
public final class DrawService {

	private final EffectRegistry effects;
	private final DeathResolver deaths = new DeathResolver();

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti, lo stesso usato per validare l'input
	 */
	public DrawService(EffectRegistry effects) {
		this.effects = effects;
	}

	/**
	 * Pesca finché esce una creatura e la mette in campo.
	 *
	 * @param state  stato della partita
	 * @param player giocatore che pesca, con il campo vuoto
	 * @return {@code true} se una creatura è entrata in campo; {@code false} se il mazzo è finito senza creature
	 */
	public boolean drawCreature(MatchState state, PlayerState player) {
		Optional<CardDefinition> drawn = player.drawCard();
		while (drawn.isPresent()) {
			CardDefinition card = drawn.get();
			if (card.type() == CardType.CREATURE) {
				CreatureInPlay creature = new CreatureInPlay(card, player.side());
				player.placeCreature(creature);
				state.log().creatureEntered(creature);
				return true;
			}
			state.log().cardDrawn(player.side(), card);
			if (card.type() == CardType.SPELL) {
				resolveSpell(state, player, card);
			}
			// TODO(@tommasofatticcioni): per ora Hidden, Terreni e Oggetti si scartano senza effetto;
			// vanno risolti con ENG-20 (#58), ENG-21 (#59) ed ENG-33 (#140)
			drawn = player.drawCard();
		}
		return false;
	}

	private void resolveSpell(MatchState state, PlayerState player, CardDefinition spell) {
		effects.createSpell(spell.effectCode()).resolve(new EffectContext(state, player.side(), spell));
		deaths.resolve(state);
	}
}
