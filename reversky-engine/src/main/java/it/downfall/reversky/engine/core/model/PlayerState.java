package it.downfall.reversky.engine.core.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Stato di un giocatore durante la partita: punti vita, mazzo da pescare, creatura in campo, Oggetto accanto al
 * mazzo e creature avversarie sconfitte.
 */
public final class PlayerState {

	private final Side side;
	private final int userId;
	private final Deque<CardDefinition> deck;
	private int lifePoints;
	private CreatureInPlay creature;
	private CardDefinition item;
	private int creaturesDefeated;

	/**
	 * Crea lo stato iniziale.
	 *
	 * @param side       lato del giocatore
	 * @param userId     id dell'utente, restituito nel risultato
	 * @param lifePoints punti vita iniziali
	 * @param deck       mazzo già mescolato, nell'ordine di pescata
	 */
	public PlayerState(Side side, int userId, int lifePoints, List<CardDefinition> deck) {
		this.side = side;
		this.userId = userId;
		this.lifePoints = lifePoints;
		this.deck = new ArrayDeque<>(deck);
	}

	/**
	 * Lato del giocatore.
	 *
	 * @return il lato
	 */
	public Side side() {
		return side;
	}

	/**
	 * Id dell'utente.
	 *
	 * @return l'id ricevuto nella richiesta
	 */
	public int userId() {
		return userId;
	}

	/**
	 * Punti vita attuali.
	 *
	 * @return i punti vita; possono essere negativi (D-43)
	 */
	public int lifePoints() {
		return lifePoints;
	}

	/**
	 * Perde punti vita.
	 *
	 * @param amount punti persi, almeno 0
	 * @throws IllegalArgumentException se {@code amount} è negativo
	 */
	public void loseLifePoints(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Punti vita persi negativi: " + amount);
		}
		lifePoints -= amount;
	}

	/**
	 * Carte ancora da pescare, nell'ordine di pescata.
	 *
	 * @return copia non modificabile
	 */
	public List<CardDefinition> deck() {
		return List.copyOf(deck);
	}

	/**
	 * Pesca la prima carta del mazzo.
	 *
	 * @return la carta, o vuoto se il mazzo è finito
	 */
	public Optional<CardDefinition> drawCard() {
		return Optional.ofNullable(deck.pollFirst());
	}

	/**
	 * Indica se nel mazzo resta almeno una creatura.
	 *
	 * @return {@code true} se si può ancora pescare una creatura
	 */
	public boolean hasCreatureInDeck() {
		return deck.stream().anyMatch(card -> card.type() == CardType.CREATURE);
	}

	/**
	 * Indica se il giocatore ha ancora creature da schierare: in campo o nel mazzo (docs/regole-base, condizioni 3
	 * e 4 di fine partita).
	 *
	 * @return {@code true} se ha una creatura in campo o nel mazzo
	 */
	public boolean hasCreatures() {
		return hasCreature() || hasCreatureInDeck();
	}

	/**
	 * Creatura in campo.
	 *
	 * @return la creatura, o vuoto se il campo è libero
	 */
	public Optional<CreatureInPlay> creature() {
		return Optional.ofNullable(creature);
	}

	/**
	 * Indica se il giocatore ha una creatura in campo.
	 *
	 * @return {@code true} se il campo è occupato
	 */
	public boolean hasCreature() {
		return creature != null;
	}

	/**
	 * Mette una creatura in campo.
	 *
	 * @param newCreature creatura di questo giocatore
	 * @throws IllegalStateException se c'è già una creatura in campo o la creatura è dell'altro giocatore
	 */
	public void placeCreature(CreatureInPlay newCreature) {
		if (creature != null) {
			throw new IllegalStateException(side + " ha già una creatura in campo: " + creature.name());
		}
		if (newCreature.side() != side) {
			throw new IllegalStateException("La creatura " + newCreature.name() + " è di " + newCreature.side());
		}
		creature = newCreature;
	}

	/**
	 * Toglie la creatura dal campo (è morta).
	 *
	 * @throws IllegalStateException se il campo è già libero
	 */
	public void removeCreature() {
		if (creature == null) {
			throw new IllegalStateException(side + " non ha una creatura in campo");
		}
		creature = null;
	}

	/**
	 * Oggetto accanto al mazzo, in attesa di scattare (D-47).
	 *
	 * @return l'Oggetto, o vuoto se non c'è o si è già consumato
	 */
	public Optional<CardDefinition> item() {
		return Optional.ofNullable(item);
	}

	/**
	 * Mette un Oggetto accanto al mazzo.
	 *
	 * @param newItem carta di tipo {@code ITEM}
	 * @throws IllegalArgumentException se la carta non è un Oggetto
	 */
	public void placeItem(CardDefinition newItem) {
		if (newItem.type() != CardType.ITEM) {
			throw new IllegalArgumentException("Non è un Oggetto: carta " + newItem.cardId());
		}
		item = newItem;
	}

	/**
	 * L'Oggetto ha applicato il suo effetto e si consuma (D-47).
	 *
	 * @throws IllegalStateException se non c'è un Oggetto
	 */
	public void consumeItem() {
		if (item == null) {
			throw new IllegalStateException(side + " non ha un Oggetto da consumare");
		}
		item = null;
	}

	/**
	 * Creature avversarie morte mentre affrontavano questo giocatore (D-42).
	 *
	 * @return il conteggio
	 */
	public int creaturesDefeated() {
		return creaturesDefeated;
	}

	/** Conta una creatura avversaria morta mentre affrontava questo giocatore, per qualunque causa (D-42). */
	public void recordDefeatedCreature() {
		creaturesDefeated++;
	}
}
