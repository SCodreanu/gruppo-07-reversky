package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;

import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
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
 * <li>Hidden: si mette coperta nella zona Hidden. Senza condizione si attiva all'inizio del prossimo scontro del
 * proprietario, con una condizione quando la condizione si verifica; una Hidden della pescata iniziale salta il primo
 * scontro (D-15). Se il giocatore ha già una Hidden attiva, la nuova aspetta in fila (ENG-41).</li>
 * <li>Terreno: entra in gioco e sostituisce quello già in gioco (D-19); il suo bonus va subito alle creature in
 * campo della sua fazione (D-17).</li>
 * <li>Oggetto: resta accanto al mazzo finché scatta, poi si consuma (D-47).</li>
 * </ul>
 * Se nel mazzo restano solo carte non creatura si risolvono tutte (D-06); la fine della partita la controlla il
 * simulatore.
 */
public final class DrawService {

	private final EffectRegistry effects;
	private final TerrainService terrains;
	private final ItemService items;
	private final DeathResolver deaths;

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti, lo stesso usato per validare l'input
	 */
	public DrawService(EffectRegistry effects) {
		this.effects = effects;
		this.terrains = new TerrainService(effects);
		this.items = new ItemService(effects);
		this.deaths = new DeathResolver(items);
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
				CreatureInPlay creature = new CreatureInPlay(card, player.side(), createEffect(card));
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
				} else {
					items.place(state, player, card);
				}
			}
			drawn = player.drawCard();
		}
		return false;
	}

	/** Una nuova istanza dell'effetto per ogni entrata in campo (D-31); le creature senza codice non ne hanno. */
	private CreatureEffect createEffect(CardDefinition creature) {
		String code = creature.effectCode();
		return code == null || code.isBlank() ? null : effects.createCreature(code);
	}

	/**
	 * D-15: la Hidden si attiva nel prossimo scontro, cioè quello che inizia dopo questa pescata. Nella pescata
	 * iniziale (nessuno scontro ancora iniziato) il prossimo è il primo, che la Hidden salta: si attiva dal secondo.
	 * ENG-41: se il giocatore ha già una Hidden attiva, la nuova aspetta in fila.
	 */
	private static void setHidden(MatchState state, PlayerState player, CardDefinition hidden) {
		int nextCombat = state.combatsStarted() + 1;
		int readyFrom = state.combatsStarted() == 0 ? nextCombat + 1 : nextCombat;
		if (player.setHidden(new SetHidden(hidden, readyFrom))) {
			state.log().hiddenSet(player.side());
		} else {
			state.log().hiddenSetBehind(player.side());
		}
	}

	/**
	 * Risolve una Spell. Una Spell {@code DAMAGE} che colpirebbe subito la creatura avversaria può essere annullata da
	 * un Fascicolo Sigillato 0 dell'avversario; senza bersaglio resta in attesa e il Fascicolo si controlla quando
	 * arriva (passo 0).
	 */
	private void resolveSpell(MatchState state, PlayerState player, CardDefinition spell) {
		PlayerState target = state.opponentOf(player.side());
		Optional<String> creature = target.creature().map(CreatureInPlay::name);
		if (spell.subtype() == CardSubtype.DAMAGE && creature.isPresent()
				&& items.cancelEnemyEffect(state, target, spell.name(), creature.get())) {
			state.log().spellResolved(player.side(), spell, "cancelled", creature.get());
			return;
		}
		effects.createSpell(spell.effectCode()).resolve(new EffectContext(state, player.side(), spell));
		deaths.resolve(state);
	}
}
