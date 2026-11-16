package it.downfall.reversky.engine.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Creatura in campo: la carta, la DEF che le resta e i modificatori (docs/carte, "Creatura").
 * <p>
 * La DEF ha due parti: la DEF permanente, che i danni riducono in modo permanente (regolamento base, punto 5), e gli
 * scudi di DEF temporanea (D-35), che assorbono i danni per primi. {@link #defense()} è la somma delle due ed è la
 * DEF che vedono le regole: la creatura muore quando è 0 o meno.
 * <p>
 * Lo stato cambia solo tramite metodi con un nome chiaro (CodingRequirements §7).
 */
public final class CreatureInPlay {

	private final CardDefinition card;
	private final Side side;
	private final List<StatModifier> modifiers = new ArrayList<>();
	private int permanentDefense;
	private int initialDefense;
	private boolean initialDefenseRecorded;

	/**
	 * Mette in campo una creatura con la DEF del catalogo.
	 *
	 * @param card carta della creatura
	 * @param side proprietario
	 * @throws IllegalArgumentException se la carta non è una creatura
	 */
	public CreatureInPlay(CardDefinition card, Side side) {
		if (card.type() != CardType.CREATURE) {
			throw new IllegalArgumentException("Solo una creatura entra in campo: carta " + card.cardId());
		}
		this.card = card;
		this.side = side;
		this.permanentDefense = card.defense();
		this.initialDefense = card.defense();
	}

	/**
	 * Carta della creatura.
	 *
	 * @return la carta
	 */
	public CardDefinition card() {
		return card;
	}

	/**
	 * Nome mostrato ai giocatori.
	 *
	 * @return il nome della carta
	 */
	public String name() {
		return card.name();
	}

	/**
	 * Proprietario della creatura.
	 *
	 * @return il lato
	 */
	public Side side() {
		return side;
	}

	/**
	 * Fazioni della carta, nell'ordine di {@link CardDefinition#factions()}.
	 *
	 * @return una o due fazioni
	 */
	public List<Integer> factions() {
		return card.factions();
	}

	/**
	 * ATK del catalogo. Non cambia per i danni.
	 *
	 * @return l'ATK base
	 */
	public int baseAttack() {
		return card.attack();
	}

	/**
	 * Somma dei modificatori di ATK attivi.
	 *
	 * @return la somma, anche negativa
	 */
	public int attackModifiers() {
		return modifiers.stream().mapToInt(StatModifier::attackBonus).sum();
	}

	/**
	 * ATK base più i modificatori, senza il bonus di fazione: quello dipende dall'avversaria e si somma nel calcolo
	 * del round, dove si applica anche il minimo 0.
	 *
	 * @return l'ATK con i modificatori, anche negativo
	 */
	public int attack() {
		return baseAttack() + attackModifiers();
	}

	/**
	 * DEF attuale: DEF permanente più gli scudi attivi.
	 *
	 * @return la DEF; 0 o meno vuol dire morta
	 */
	public int defense() {
		return permanentDefense + modifiers.stream().mapToInt(StatModifier::shieldValue).sum();
	}

	/**
	 * DEF all'entrata in campo, bonus compresi (M-06): serve alle soglie "metà DEF" e ai tetti di {@code heal}.
	 * Prima di {@link #recordInitialDefense()} è la DEF del catalogo.
	 *
	 * @return la DEF all'entrata
	 */
	public int initialDefense() {
		return initialDefense;
	}

	/**
	 * Fissa la DEF all'entrata alla DEF attuale. Si chiama alla fine dell'inizio del primo scontro della creatura,
	 * quando i bonus d'entrata sono stati applicati; le chiamate successive non cambiano nulla.
	 */
	public void recordInitialDefense() {
		if (!initialDefenseRecorded) {
			initialDefense = defense();
			initialDefenseRecorded = true;
		}
	}

	/**
	 * Indica se la creatura è morta.
	 *
	 * @return {@code true} se la DEF è 0 o meno (regolamento base, punto 3; D-16)
	 */
	public boolean isDead() {
		return defense() <= 0;
	}

	/**
	 * Aggiunge DEF permanente (es. bonus DEF di fazione a inizio scontro, Spell {@code SHIELD}): resta anche negli
	 * scontri successivi e i danni la consumano come la DEF del catalogo.
	 *
	 * @param amount DEF da aggiungere, almeno 0
	 * @throws IllegalArgumentException se {@code amount} è negativo: una perdita di DEF è un danno
	 */
	public void addDefense(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Per togliere DEF si usa takeDamage: " + amount);
		}
		permanentDefense += amount;
	}

	/**
	 * Subisce un danno: prima lo assorbono gli scudi, dal primo che scade, poi la DEF permanente, che può scendere
	 * sotto 0 (il danno al giocatore è la DEF negativa, D-03).
	 *
	 * @param damage danno, almeno 0
	 * @throws IllegalArgumentException se {@code damage} è negativo
	 */
	public void takeDamage(int damage) {
		if (damage < 0) {
			throw new IllegalArgumentException("Danno negativo: " + damage);
		}
		int remaining = damage;
		for (StatModifier shield : shieldsInAbsorbOrder()) {
			remaining = shield.absorb(remaining);
		}
		modifiers.removeIf(StatModifier::isExhausted);
		permanentDefense -= remaining;
	}

	/**
	 * Aggiunge un modificatore di ATK o uno scudo.
	 *
	 * @param modifier il modificatore
	 */
	public void addModifier(StatModifier modifier) {
		modifiers.add(modifier);
	}

	/**
	 * Toglie tutti i modificatori di un'origine (es. il Terreno esce, D-19). Di uno scudo sparisce solo la parte non
	 * usata, quindi togliere modificatori non uccide mai.
	 *
	 * @param source origine da togliere
	 */
	public void removeModifiers(ModifierSource source) {
		modifiers.removeIf(modifier -> modifier.source() == source);
	}

	/**
	 * Modificatori attuali, nell'ordine in cui sono stati aggiunti.
	 *
	 * @return vista non modificabile
	 */
	public List<StatModifier> modifiers() {
		return Collections.unmodifiableList(modifiers);
	}

	/** Fine di un round risolto (dopo il passo 6): scadono i modificatori e le sospensioni a round. */
	public void endRound() {
		modifiers.removeIf(StatModifier::endRound);
	}

	/**
	 * Fine dello scontro (una delle due creature è morta): scadono i modificatori e le sospensioni fino a fine
	 * scontro.
	 */
	public void endCombat() {
		modifiers.removeIf(StatModifier::endCombat);
	}

	private List<StatModifier> shieldsInAbsorbOrder() {
		// List.sort è stabile: a parità di scadenza assorbe prima lo scudo aggiunto prima
		List<StatModifier> shields = new ArrayList<>(modifiers.stream()
				.filter(modifier -> modifier.shieldValue() > 0)
				.toList());
		shields.sort(Comparator.comparingLong(StatModifier::expiryRank));
		return shields;
	}
}
