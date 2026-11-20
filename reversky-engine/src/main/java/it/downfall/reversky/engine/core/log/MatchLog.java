package it.downfall.reversky.engine.core.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.downfall.reversky.engine.core.model.BattleResult;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.EndReason;
import it.downfall.reversky.engine.core.model.PendingModifier;
import it.downfall.reversky.engine.core.model.Side;

/**
 * Log di una partita: la lista ordinata degli eventi restituita all'API (docs/log-partita).
 * <p>
 * Gli eventi si aggiungono solo da qui, con un metodo per ogni {@link EventType} (regola 2 di docs/log-partita).
 * Il log tiene il round corrente: {@link #roundStarted(int)} lo aggiorna e ogni evento successivo lo riporta; gli
 * eventi della preparazione sono al round 0. I {@code data} hanno chiavi fisse e valori senza testo; i messaggi sono
 * in inglese, perché li leggono i giocatori.
 * <p>
 * I numeri negativi nei messaggi usano il segno meno tipografico (−), come negli esempi di docs/log-partita.
 */
public final class MatchLog {

	private static final char MINUS = '−';

	private final List<MatchEvent> events = new ArrayList<>();
	private int round;

	/**
	 * Crea un log vuoto, al round 0. Ogni partita ha il suo.
	 */
	public MatchLog() {
	}

	/**
	 * Eventi registrati finora, in ordine.
	 *
	 * @return copia non modificabile
	 */
	public List<MatchEvent> events() {
		return List.copyOf(events);
	}

	/**
	 * Round corrente; 0 durante la preparazione.
	 *
	 * @return il round degli eventi che si aggiungono ora
	 */
	public int round() {
		return round;
	}

	/**
	 * {@code MATCH_STARTED}: inizio della partita.
	 *
	 * @param startingLifePoints punti vita iniziali di ogni giocatore
	 */
	public void matchStarted(int startingLifePoints) {
		add(EventType.MATCH_STARTED, null, data("startingLifePoints", startingLifePoints),
				"Match started — " + number(startingLifePoints) + " life points each");
	}

	/**
	 * {@code CARD_DRAWN}: pescata di una Spell, un Terreno o un Oggetto (per le creature c'è
	 * {@link #creatureEntered}, D-07).
	 *
	 * @param side giocatore che pesca
	 * @param card carta pescata
	 */
	public void cardDrawn(Side side, CardDefinition card) {
		add(EventType.CARD_DRAWN, side, data("card", card.name(), "cardType", card.type().name()),
				player(side) + " draws " + card.name() + " (" + card.type().name() + ")");
	}

	/**
	 * {@code CREATURE_ENTERED}: una creatura entra in campo.
	 *
	 * @param creature creatura appena entrata, con ATK e DEF attuali
	 */
	public void creatureEntered(CreatureInPlay creature) {
		add(EventType.CREATURE_ENTERED, creature.side(),
				data("card", creature.name(), "attack", creature.attack(), "defense", creature.defense()),
				player(creature.side()) + " plays " + creature.name() + " (ATK " + number(creature.attack())
						+ ", DEF " + number(creature.defense()) + ")");
	}

	/**
	 * {@code SPELL_RESOLVED}: una Spell viene risolta e scartata.
	 *
	 * @param side   proprietario della Spell
	 * @param spell  Spell risolta
	 * @param effect descrizione dell'effetto applicato, senza testo per i giocatori (es. codice o valori)
	 * @param target nome della creatura colpita o potenziata; {@code null} se si applica alla prossima creatura
	 */
	public void spellResolved(Side side, CardDefinition spell, String effect, String target) {
		String message = target == null ? spell.name() + " resolves" : spell.name() + " resolves on " + target;
		add(EventType.SPELL_RESOLVED, side, data("card", spell.name(), "effect", effect, "target", target), message);
	}

	/**
	 * {@code BURN_REVEALED}: una Spell {@code BURN} rivela la prossima carta del mazzo (D-13).
	 *
	 * @param side         proprietario della Spell
	 * @param burn         Spell {@code BURN}
	 * @param revealed     carta rivelata; {@code null} se il mazzo è vuoto
	 * @param bonusApplied {@code true} se la carta rivelata è una creatura e riceverà il bonus
	 */
	public void burnRevealed(Side side, CardDefinition burn, CardDefinition revealed, boolean bonusApplied) {
		String revealedName = revealed == null ? null : revealed.name();
		String message = burn.name() + " reveals " + (revealedName == null ? "nothing" : revealedName);
		add(EventType.BURN_REVEALED, side,
				data("card", burn.name(), "revealed", revealedName, "bonusApplied", bonusApplied), message);
	}

	/**
	 * {@code HIDDEN_SET}: una Hidden viene messa coperta; la carta non si nomina, resta nascosta.
	 *
	 * @param side giocatore che la mette coperta
	 */
	public void hiddenSet(Side side) {
		add(EventType.HIDDEN_SET, side, data(), player(side) + " sets a Hidden card");
	}

	/**
	 * {@code HIDDEN_TRIGGERED}: una Hidden si attiva.
	 *
	 * @param side   proprietario della Hidden
	 * @param hidden Hidden attivata
	 * @param effect descrizione dell'effetto applicato
	 */
	public void hiddenTriggered(Side side, CardDefinition hidden, String effect) {
		add(EventType.HIDDEN_TRIGGERED, side, data("card", hidden.name(), "effect", effect),
				hidden.name() + " triggers");
	}

	/**
	 * {@code TERRAIN_PLACED}: un Terreno entra in gioco, sostituendo quello già in gioco (D-19).
	 *
	 * @param side     giocatore che l'ha pescato
	 * @param terrain  Terreno nuovo
	 * @param replaced Terreno sostituito; {@code null} se non ce n'era uno
	 */
	public void terrainPlaced(Side side, CardDefinition terrain, CardDefinition replaced) {
		String replacedName = replaced == null ? null : replaced.name();
		String message = terrain.name() + " enters play" + (replacedName == null ? "" : ", replacing " + replacedName);
		add(EventType.TERRAIN_PLACED, side, data("card", terrain.name(), "replaced", replacedName), message);
	}

	/**
	 * {@code TERRAIN_REMOVED}: un effetto toglie il Terreno in gioco.
	 *
	 * @param side    giocatore della carta che lo toglie
	 * @param terrain Terreno tolto
	 * @param cause   nome della carta che lo toglie
	 */
	public void terrainRemoved(Side side, CardDefinition terrain, String cause) {
		add(EventType.TERRAIN_REMOVED, side, data("card", terrain.name(), "cause", cause),
				terrain.name() + " is removed by " + cause);
	}

	/**
	 * {@code DECK_REORDERED}: un effetto sposta una carta nel mazzo di un giocatore.
	 *
	 * @param side     proprietario del mazzo
	 * @param source   nome della carta che sposta
	 * @param card     carta spostata
	 * @param position nuova posizione nel mazzo, da 1
	 */
	public void deckReordered(Side side, String source, CardDefinition card, int position) {
		add(EventType.DECK_REORDERED, side, data("source", source, "card", card.name(), "position", position),
				source + " moves " + card.name() + " to position " + number(position));
	}

	/**
	 * {@code ITEM_PLACED}: un Oggetto entra in gioco accanto al mazzo.
	 *
	 * @param side giocatore che l'ha pescato
	 * @param item Oggetto
	 */
	public void itemPlaced(Side side, CardDefinition item) {
		add(EventType.ITEM_PLACED, side, data("card", item.name()), player(side) + " places " + item.name());
	}

	/**
	 * {@code ITEM_CONSUMED}: un Oggetto applica il suo effetto e si consuma (D-47).
	 *
	 * @param side     proprietario dell'Oggetto
	 * @param item     Oggetto
	 * @param effect   descrizione dell'effetto applicato
	 * @param creature nome della creatura su cui si applica
	 */
	public void itemConsumed(Side side, CardDefinition item, String effect, String creature) {
		add(EventType.ITEM_CONSUMED, side, data("card", item.name(), "effect", effect, "creature", creature),
				item.name() + " is used on " + creature);
	}

	/**
	 * {@code ROUND_STARTED}: inizio di un round. Da qui gli eventi riportano questo round.
	 *
	 * @param newRound numero del round, da 1
	 */
	public void roundStarted(int newRound) {
		this.round = newRound;
		add(EventType.ROUND_STARTED, null, data("round", newRound), "Round " + number(newRound));
	}

	/**
	 * {@code ATTACK_COMPUTED}: calcolo dell'attacco di una creatura.
	 *
	 * @param attacker     creatura che attacca
	 * @param factionBonus bonus ATK di fazione contro l'avversaria, anche negativo
	 * @param effectBonus  modifica dell'effetto dell'attaccante ({@code modifyOutgoingAttack}), anche negativa
	 * @param frozen       attacco congelato (Yuki-onna): il totale è 0 qualunque siano le altre parti
	 * @param total        attacco effettivo, già con il minimo 0
	 */
	public void attackComputed(CreatureInPlay attacker, int factionBonus, int effectBonus, boolean frozen,
			int total) {
		int base = attacker.baseAttack();
		int modifiers = attacker.attackModifiers();
		StringBuilder parts = new StringBuilder(number(base)).append(" base");
		if (factionBonus != 0) {
			parts.append(", ").append(signed(factionBonus)).append(" faction");
		}
		if (modifiers != 0) {
			parts.append(", ").append(signed(modifiers)).append(" modifiers");
		}
		if (effectBonus != 0) {
			parts.append(", ").append(signed(effectBonus)).append(" effect");
		}
		if (frozen) {
			parts.append(", frozen");
		}
		add(EventType.ATTACK_COMPUTED, attacker.side(),
				data("attacker", attacker.name(), "baseAttack", base, "factionBonus", factionBonus,
						"modifiers", modifiers, "effectBonus", effectBonus, "frozen", frozen, "total", total),
				attacker.name() + " attacks with " + number(total) + " (" + parts + ")");
	}

	/**
	 * {@code DAMAGE_DEALT}: una creatura colpisce l'altra. Si chiama dopo aver applicato il danno.
	 *
	 * @param source creatura che colpisce
	 * @param target creatura colpita, con la DEF dopo il danno
	 * @param amount danno inflitto
	 */
	public void damageDealt(CreatureInPlay source, CreatureInPlay target, int amount) {
		add(EventType.DAMAGE_DEALT, source.side(),
				data("source", source.name(), "target", target.name(), "amount", amount,
						"targetDefense", target.defense()),
				source.name() + " deals " + number(amount) + " damage to " + target.name() + " (DEF "
						+ number(target.defense()) + ")");
	}

	/**
	 * {@code EFFECT_TRIGGERED}: un'abilità agisce (anche il bonus DEF di fazione a inizio scontro).
	 *
	 * @param side        proprietario della carta
	 * @param card        nome della carta
	 * @param description cosa fa, in inglese
	 */
	public void effectTriggered(Side side, String card, String description) {
		add(EventType.EFFECT_TRIGGERED, side, data("card", card, "description", description),
				card + ": " + description);
	}

	/**
	 * {@code CREATURE_DIED}: una creatura muore.
	 *
	 * @param creature creatura morta
	 */
	public void creatureDied(CreatureInPlay creature) {
		add(EventType.CREATURE_DIED, creature.side(), data("card", creature.name()),
				creature.name() + " is destroyed");
	}

	/**
	 * {@code PLAYER_DAMAGED}: un giocatore perde punti vita.
	 *
	 * @param side       giocatore colpito
	 * @param amount     punti vita persi
	 * @param lifePoints punti vita rimasti, anche negativi (D-43)
	 */
	public void playerDamaged(Side side, int amount, int lifePoints) {
		add(EventType.PLAYER_DAMAGED, side, data("amount", amount, "lifePoints", lifePoints),
				player(side) + " loses " + number(amount) + " life points (" + number(lifePoints) + " left)");
	}

	/**
	 * {@code MATCH_ENDED}: fine della partita.
	 *
	 * @param result    esito
	 * @param endReason motivo di fine
	 */
	public void matchEnded(BattleResult result, EndReason endReason) {
		String outcome = switch (result) {
			case CHALLENGER_WIN -> "Challenger wins";
			case OPPONENT_WIN -> "Opponent wins";
			case DRAW -> "Draw";
		};
		String reason = endReason.name().toLowerCase(Locale.ROOT).replace('_', ' ');
		add(EventType.MATCH_ENDED, null, data("result", result.name(), "endReason", endReason.name()),
				outcome + " (" + reason + ")");
	}

	private void add(EventType type, Side side, Map<String, Object> data, String message) {
		events.add(new MatchEvent(events.size() + 1, round, type, side, data, message));
	}

	/** Coppie chiave-valore nell'ordine dato; i valori possono essere nulli (es. nessun Terreno sostituito). */
	private static Map<String, Object> data(Object... keysAndValues) {
		Map<String, Object> data = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			data.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return data;
	}

	/**
	 * Nome di un giocatore nei messaggi del log.
	 *
	 * @param side il lato
	 * @return "Challenger" o "Opponent"
	 */
	public static String player(Side side) {
		return side == Side.CHALLENGER ? "Challenger" : "Opponent";
	}

	/**
	 * Un modificatore in attesa come nei messaggi del log (es. {@code +2 ATK, +2 DEF} o {@code −5 DEF}).
	 *
	 * @param modifier il modificatore
	 * @return il testo
	 */
	public static String describe(PendingModifier modifier) {
		List<String> parts = new ArrayList<>();
		if (modifier.attack() != 0) {
			parts.add(signed(modifier.attack()) + " ATK");
		}
		if (modifier.shield() > 0) {
			parts.add(signed(modifier.shield()) + " DEF");
		}
		if (modifier.damage() > 0) {
			parts.add(signed(-modifier.damage()) + " DEF");
		}
		if (modifier.ward() > 0) {
			parts.add(signed(-modifier.ward()) + " to the first damage");
		}
		return String.join(", ", parts);
	}

	/**
	 * Un numero come nei messaggi del log: i negativi con il segno meno tipografico (es. {@code −3}).
	 *
	 * @param value il numero
	 * @return il testo
	 */
	public static String number(int value) {
		return value < 0 ? MINUS + Integer.toString(-value) : Integer.toString(value);
	}

	/**
	 * Un bonus o un malus come nei messaggi del log: sempre con il segno (es. {@code +2}, {@code −3}).
	 *
	 * @param value il valore
	 * @return il testo
	 */
	public static String signed(int value) {
		return value < 0 ? number(value) : "+" + value;
	}
}
