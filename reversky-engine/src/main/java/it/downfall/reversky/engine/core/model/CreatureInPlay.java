package it.downfall.reversky.engine.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import it.downfall.reversky.engine.core.effect.CreatureEffect;

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
	/** Effetto della creatura, una nuova istanza per ogni entrata in campo (D-31), o {@code null} se non ne ha. */
	private final CreatureEffect effect;
	/** Effetti collegati da altre carte (SCP-109, ENG-34), nell'ordine in cui sono arrivati. */
	private final List<CreatureEffect> attachedEffects = new ArrayList<>();
	private final List<StatModifier> modifiers = new ArrayList<>();
	/** Ferite non ancora riferite all'effetto, nell'ordine in cui sono avvenute (deciso il 18 novembre). */
	private final List<Wound> unreportedWounds = new ArrayList<>();
	private int permanentDefense;
	private int initialDefense;
	private boolean initialDefenseRecorded;
	/** Danni presi prima di fissare la DEF all'entrata: non la riducono (M-06, deciso il 18 novembre). */
	private int damageBeforeEntry;
	/** DEF permanente persa davvero dall'entrata in campo, somma delle ferite. */
	private int woundsTaken;
	private boolean combatStarted;
	private boolean entered;
	/** ATK base preso da un effetto (SCP-2006), o {@code null}: allora vale l'ATK del catalogo. */
	private Integer copiedBaseAttack;
	/** Il Terreno in gioco che le ha dato il bonus (D-17), o {@code null}: il bonus si riceve una volta per Terreno. */
	private PlacedTerrain terrainBonus;
	/** Scudo a inizio round programmato da un Oggetto (Bussola dei Sentieri Falsi), o {@code null}. */
	private RoundStartShield roundStartShield;

	/**
	 * Mette in campo una creatura senza effetto, con la DEF del catalogo.
	 *
	 * @param card carta della creatura
	 * @param side proprietario
	 * @throws IllegalArgumentException se la carta non è una creatura
	 */
	public CreatureInPlay(CardDefinition card, Side side) {
		this(card, side, null);
	}

	/**
	 * Mette in campo una creatura con il suo effetto, con la DEF del catalogo.
	 *
	 * @param card   carta della creatura
	 * @param side   proprietario
	 * @param effect nuova istanza dell'effetto della carta (D-31), o {@code null} se la carta non ha effetto
	 * @throws IllegalArgumentException se la carta non è una creatura
	 */
	public CreatureInPlay(CardDefinition card, Side side, CreatureEffect effect) {
		if (card.type() != CardType.CREATURE) {
			throw new IllegalArgumentException("Solo una creatura entra in campo: carta " + card.cardId());
		}
		this.card = card;
		this.side = side;
		this.effect = effect;
		this.permanentDefense = card.defense();
	}

	/**
	 * Effetto della creatura.
	 *
	 * @return l'effetto, o vuoto se la carta non ne ha (es. le Common)
	 */
	public Optional<CreatureEffect> effect() {
		return Optional.ofNullable(effect);
	}

	/**
	 * Tutti gli effetti che il motore chiama per questa creatura (ENG-34, deciso il 18 novembre): prima il suo, poi
	 * quelli collegati da altre carte, nell'ordine in cui sono arrivati.
	 *
	 * @return gli effetti, anche nessuno
	 */
	public List<CreatureEffect> effects() {
		List<CreatureEffect> effects = new ArrayList<>();
		effect().ifPresent(effects::add);
		effects.addAll(attachedEffects);
		return effects;
	}

	/**
	 * Collega alla creatura l'effetto di un'altra carta finché resta in campo (es. SCP-109, ENG-34). Non è l'effetto
	 * della creatura: annullare il suo effetto non lo tocca.
	 *
	 * @param attached nuova istanza dell'effetto, solo per questa creatura
	 */
	public void attachEffect(CreatureEffect attached) {
		attachedEffects.add(Objects.requireNonNull(attached, "attached"));
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
	 * ATK base: quello del catalogo, o quello preso da un effetto finché la creatura resta in campo (SCP-2006). Non
	 * cambia per i danni e non è un bonus ATK.
	 *
	 * @return l'ATK base
	 */
	public int baseAttack() {
		return copiedBaseAttack != null ? copiedBaseAttack : card.attack();
	}

	/**
	 * Prende un altro ATK base finché resta in campo (SCP-2006, deciso il 18 novembre): non è un modificatore, quindi
	 * gli effetti che sospendono i bonus ATK non lo toccano.
	 *
	 * @param attack nuovo ATK base, almeno 0
	 * @throws IllegalArgumentException se {@code attack} è negativo
	 */
	public void copyBaseAttack(int attack) {
		if (attack < 0) {
			throw new IllegalArgumentException("ATK base negativo: " + attack);
		}
		this.copiedBaseAttack = attack;
	}

	/**
	 * Somma dei modificatori di ATK attivi: i bonus meno i punti sigillati (mai sotto 0, ENG-35), più i malus.
	 *
	 * @return la somma, anche negativa
	 */
	public int attackModifiers() {
		int penalties = modifiers.stream().mapToInt(StatModifier::attackBonus).filter(value -> value < 0).sum();
		return effectiveAttackBonus() + penalties;
	}

	/**
	 * Bonus ATK che contano davvero (ENG-35, deciso il 18 novembre): la somma dei modificatori di ATK positivi attivi,
	 * meno i sigilli, mai sotto 0. Se è 0 la creatura "non ha bonus ATK" (L'Addetto alle Uscite, Static
	 * Interference).
	 *
	 * @return i bonus ATK effettivi, almeno 0
	 */
	public int effectiveAttackBonus() {
		int bonuses = modifiers.stream().mapToInt(StatModifier::attackBonus).filter(value -> value > 0).sum();
		int sealed = modifiers.stream().mapToInt(StatModifier::sealValue).sum();
		return Math.max(0, bonuses - sealed);
	}

	/**
	 * Toglie per sempre tutti i bonus (SCP-3999, ENG-35, deciso il 18 novembre): i modificatori di ATK positivi e gli
	 * scudi, di qualunque origine, anche quello di un Terreno, che non torna finché non arriva un altro Terreno.
	 * Restano malus, sigilli, congelamenti e la DEF permanente (bonus di fazione compreso); dello scudo sparisce solo
	 * la parte non usata, quindi non uccide mai.
	 *
	 * @return {@code true} se ha tolto almeno un modificatore
	 */
	public boolean removeBonuses() {
		return modifiers.removeIf(StatModifier::isBonus);
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
	 * DEF all'entrata in campo, bonus compresi e danni esclusi (M-06, deciso il 18 novembre): serve alle soglie "metà
	 * DEF", a Hounds e ai tetti di {@code heal}. I danni presi prima di fissarla (Spell e Hidden {@code DAMAGE}, danni
	 * in attesa, effetti d'entrata avversari) non la riducono. Prima di {@link #recordInitialDefense()} si calcola
	 * sulla DEF attuale, quindi vale già durante l'inizio dello scontro.
	 *
	 * @return la DEF all'entrata
	 */
	public int initialDefense() {
		return initialDefenseRecorded ? initialDefense : defense() + damageBeforeEntry;
	}

	/**
	 * DEF permanente persa davvero dall'entrata in campo: la somma delle ferite, senza la parte assorbita dagli scudi
	 * (The Unseen Matriarch).
	 *
	 * @return la DEF persa, almeno 0
	 */
	public int woundsTaken() {
		return woundsTaken;
	}

	/**
	 * Indica se ci sono ferite non ancora riferite all'effetto.
	 *
	 * @return {@code true} se ce n'è almeno una
	 */
	public boolean hasUnreportedWounds() {
		return !unreportedWounds.isEmpty();
	}

	/**
	 * Prende le ferite non ancora riferite, nell'ordine in cui sono avvenute, e le toglie dalla coda: il motore le
	 * riferisce all'effetto al controllo dopo la fase (deciso il 18 novembre).
	 *
	 * @return le ferite, anche nessuna
	 */
	public List<Wound> takeUnreportedWounds() {
		List<Wound> wounds = List.copyOf(unreportedWounds);
		unreportedWounds.clear();
		return wounds;
	}

	/**
	 * Indica se l'attacco è congelato (Yuki-onna): allora nel calcolo del round vale 0.
	 *
	 * @return {@code true} se c'è un congelamento dell'attacco attivo
	 */
	public boolean isAttackFrozen() {
		return modifiers.stream().anyMatch(StatModifier::freezesAttack);
	}

	/**
	 * Indica se la creatura ha già avuto un inizio di scontro. Una creatura appena entrata in campo non l'ha ancora
	 * avuto: riceve i modificatori in attesa del proprietario (D-34).
	 *
	 * @return {@code true} dopo {@link #recordCombatStarted()}
	 */
	public boolean hasStartedCombat() {
		return combatStarted;
	}

	/** Ricorda che la creatura ha avuto un inizio di scontro (fine del passo 0, anche se lo scontro è finito lì). */
	public void recordCombatStarted() {
		combatStarted = true;
	}

	/**
	 * Indica se il suo {@code onEnterPlay} è già stato chiamato. Di solito succede al primo inizio di scontro; se una
	 * Hidden uccide prima, succede allo scontro dopo (deciso il 18 novembre).
	 *
	 * @return {@code true} dopo {@link #recordEntered()}
	 */
	public boolean hasEntered() {
		return entered;
	}

	/** Ricorda che il momento dell'entrata in campo è passato (una volta per entrata, D-31). */
	public void recordEntered() {
		entered = true;
	}

	/**
	 * Fissa la DEF all'entrata: la DEF attuale più i danni presi fin qui (M-06, deciso il 18 novembre). Si chiama
	 * alla fine dell'inizio dello scontro in cui la creatura entra davvero ({@link #hasEntered()}), quando i bonus
	 * d'entrata sono stati applicati; le chiamate successive non cambiano nulla.
	 */
	public void recordInitialDefense() {
		if (!initialDefenseRecorded) {
			initialDefense = defense() + damageBeforeEntry;
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
	 * Recupera DEF (ENG-34, deciso il 18 novembre): DEF permanente in più, senza superare la DEF all'entrata, scudi
	 * compresi; uno scudo scaduto si può quindi recuperare. Una creatura con DEF 0 o meno è morta e non recupera
	 * niente (D-16). Le ferite già subite restano nel conto di {@link #woundsTaken()}.
	 *
	 * @param amount DEF da recuperare, almeno 0
	 * @return la DEF recuperata davvero, almeno 0
	 * @throws IllegalArgumentException se {@code amount} è negativo
	 */
	public int heal(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Recupero negativo: " + amount);
		}
		if (isDead()) {
			return 0;
		}
		int healed = Math.min(amount, Math.max(0, initialDefense() - defense()));
		permanentDefense += healed;
		return healed;
	}

	/**
	 * Subisce un danno da una carta che non è una creatura (Spell, Hidden, Terreno, Oggetto) o da un modificatore in
	 * attesa: come {@link #takeDamage(int, CreatureInPlay)}, ma la ferita non ha una creatura come causa.
	 *
	 * @param damage danno, almeno 0
	 * @return la ferita: la DEF persa davvero sopra 0, dopo gli scudi
	 * @throws IllegalArgumentException se {@code damage} è negativo
	 */
	public int takeDamage(int damage) {
		return applyDamage(damage, null);
	}

	/**
	 * Subisce un danno da una creatura, con il suo attacco o con il suo effetto: prima lo assorbono gli scudi, dal
	 * primo che scade, poi la DEF permanente, che può scendere sotto 0 (il danno al giocatore è la DEF negativa,
	 * D-03). Se la DEF permanente scende, la ferita resta in coda finché il motore la riferisce all'effetto.
	 *
	 * @param damage danno, almeno 0
	 * @param cause  creatura che infligge il danno
	 * @return la ferita (docs/effetti, "Soglie"): la DEF persa davvero sopra 0, dopo gli scudi
	 * @throws IllegalArgumentException se {@code damage} è negativo
	 */
	public int takeDamage(int damage, CreatureInPlay cause) {
		return applyDamage(damage, Objects.requireNonNull(cause, "cause"));
	}

	private int applyDamage(int damage, CreatureInPlay cause) {
		if (damage < 0) {
			throw new IllegalArgumentException("Danno negativo: " + damage);
		}
		if (!initialDefenseRecorded) {
			// gli scudi assorbono e la DEF permanente scende: la DEF cala sempre esattamente del danno
			damageBeforeEntry += damage;
		}
		int remaining = damage;
		for (StatModifier shield : shieldsInAbsorbOrder()) {
			remaining = shield.absorb(remaining);
		}
		modifiers.removeIf(StatModifier::isExhausted);
		int wound = Math.min(remaining, Math.max(0, permanentDefense));
		permanentDefense -= remaining;
		if (wound > 0) {
			woundsTaken += wound;
			unreportedWounds.add(new Wound(wound, cause));
		}
		return wound;
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
		// "nei primi round dello scontro": se lo scontro finisce prima, i round restanti si perdono
		roundStartShield = null;
	}

	private List<StatModifier> shieldsInAbsorbOrder() {
		// List.sort è stabile: a parità di scadenza assorbe prima lo scudo aggiunto prima
		List<StatModifier> shields = new ArrayList<>(modifiers.stream()
				.filter(modifier -> modifier.shieldValue() > 0)
				.toList());
		shields.sort(Comparator.comparingLong(StatModifier::expiryRank));
		return shields;
	}

	/**
	 * Indica se la creatura ha già ricevuto il bonus di un Terreno messo in gioco. Conta la singola messa in gioco
	 * (stesso oggetto), non la carta: il bonus si riceve una volta sola anche se lo scudo del Terreno si consuma.
	 *
	 * @param terrain il Terreno in gioco
	 * @return {@code true} se ha già il bonus di quel Terreno
	 */
	public boolean hasTerrainBonusFrom(PlacedTerrain terrain) {
		return terrainBonus == terrain;
	}

	/**
	 * Ricorda che la creatura ha ricevuto il bonus di un Terreno (i modificatori li aggiunge chi chiama).
	 *
	 * @param terrain il Terreno in gioco
	 */
	public void recordTerrainBonus(PlacedTerrain terrain) {
		this.terrainBonus = terrain;
	}

	/**
	 * Il Terreno esce: si tolgono i suoi modificatori, l'ATK subito e dello scudo solo la parte non ancora consumata
	 * (D-19, D-35), quindi l'uscita di un Terreno non uccide mai.
	 *
	 * @return {@code true} se la creatura aveva il bonus di un Terreno
	 */
	public boolean loseTerrainBonus() {
		if (terrainBonus == null) {
			return false;
		}
		removeModifiers(ModifierSource.TERRAIN);
		terrainBonus = null;
		return true;
	}

	/**
	 * La creatura sopravvive a un danno letale e resta con la DEF data (Badge di Uscita Negata, D-36): la DEF
	 * permanente diventa quella che serve perché, con gli scudi rimasti, la DEF sia proprio quella.
	 *
	 * @param defense DEF con cui resta, almeno 1
	 * @throws IllegalArgumentException se {@code defense} è minore di 1
	 */
	public void surviveWith(int defense) {
		if (defense < 1) {
			throw new IllegalArgumentException("Per sopravvivere serve almeno 1 DEF: " + defense);
		}
		permanentDefense = defense - modifiers.stream().mapToInt(StatModifier::shieldValue).sum();
	}

	/**
	 * Programma uno scudo all'inizio di ognuno dei prossimi round dello scontro (D-35): ogni round lo scudo torna al
	 * suo valore, non si somma. Finisce con lo scontro.
	 *
	 * @param shield programma dello scudo
	 */
	public void scheduleRoundStartShield(RoundStartShield shield) {
		this.roundStartShield = shield;
	}

	/**
	 * Inizio di un round (passo 1): se c'è uno scudo programmato, la creatura riceve lo scudo per questo round.
	 *
	 * @return lo scudo dato, o vuoto se non ce n'era uno
	 */
	public Optional<StatModifier> startRound() {
		if (roundStartShield == null) {
			return Optional.empty();
		}
		StatModifier shield = StatModifier.shield(roundStartShield.source(), roundStartShield.side(),
				roundStartShield.amount(), ModifierDuration.rounds(1));
		modifiers.add(shield);
		roundStartShield = roundStartShield.next();
		return Optional.of(shield);
	}
}
