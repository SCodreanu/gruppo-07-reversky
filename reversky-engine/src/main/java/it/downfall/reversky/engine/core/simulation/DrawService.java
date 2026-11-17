package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.SetHidden;

/**
 * Pescata (regole-base, "Pescare"): un giocatore con il campo vuoto pesca finché esce una creatura, che entra in campo.
 * Le carte non creatura pescate prima si risolvono subito, nell'ordine in cui escono.
 * <ul>
 * <li>Spell: l'effetto si risolve e la carta si scarta (D-10). Una Spell {@code DAMAGE} può uccidere la creatura
 * avversaria: le morti si controllano subito dopo (D-08).</li>
 * <li>Hidden: si mette coperta nella zona Hidden e si attiverà all'inizio del prossimo scontro del proprietario; una
 * Hidden della pescata iniziale salta il primo scontro (D-15).</li>
 * <li>Terreno: entra in gioco e sostituisce quello già in gioco (D-19); il suo bonus va subito alle creature in
 * campo della sua fazione (D-17).</li>
 * <li>Oggetti: per ora si registrano e si scartano (ENG-33).</li>
 * </ul>
 * Se nel mazzo restano solo carte non creatura si risolvono tutte (D-06); la fine della partita la controlla il
 * simulatore.
 */
public final class DrawService {

	private final EffectRegistry effects;
	private final TerrainService terrains;
	private final DeathResolver deaths = new DeathResolver();

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti, lo stesso usato per validare l'input
	 */
	public DrawService(EffectRegistry effects) {
		this.effects = effects;
		this.terrains = new TerrainService(effects);
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
			if (card.type() == CardType.HIDDEN) {
				// Una Hidden resta coperta: il log non la nomina finché non si attiva (HIDDEN_SET, niente CARD_DRAWN)
				setHidden(state, player, card);
			} else {
				state.log().cardDrawn(player.side(), card);
				if (card.type() == CardType.SPELL) {
					resolveSpell(state, player, card);
				} else if (card.type() == CardType.TERRAIN) {
					terrains.place(state, player, card);
				}
			}
			// TODO(@tommasofatticcioni): per ora gli Oggetti si scartano senza effetto; vanno risolti con ENG-33 (#140)
			drawn = player.drawCard();
		}
		return false;
	}

	/**
	 * D-15: la Hidden si attiva nel prossimo scontro, cioè quello che inizia dopo questa pescata. Nella pescata
	 * iniziale (nessuno scontro ancora iniziato) il prossimo è il primo, che la Hidden salta: si attiva dal secondo.
	 */
	private static void setHidden(MatchState state, PlayerState player, CardDefinition hidden) {
		int nextCombat = state.combatsStarted() + 1;
		int readyFrom = state.combatsStarted() == 0 ? nextCombat + 1 : nextCombat;
		player.setHidden(new SetHidden(hidden, readyFrom));
		state.log().hiddenSet(player.side());
	}

	private void resolveSpell(MatchState state, PlayerState player, CardDefinition spell) {
		effects.createSpell(spell.effectCode()).resolve(new EffectContext(state, player.side(), spell));
		deaths.resolve(state);
	}
}
