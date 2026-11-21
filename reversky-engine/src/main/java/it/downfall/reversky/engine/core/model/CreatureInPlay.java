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
	/** Effetto preso in prestito da un'altra creatura (SCP-2521, Kitsune, ENG-38), o {@code null}. */
	private CreatureEffect borrowedEffect;
	/** Quanto dura il prestito: per tutta la permanenza o fino a fine scontro. */
	private ModifierDuration.Kind borrowedUntil;
	/** Annullamento del suo effetto (ENG-38): per tutta la permanenza o fino a fine scontro, o {@code null}. */
	private ModifierDuration.Kind cancelledUntil;
	/** Bonus ATK che il suo effetto (o quello in prestito) le ha dato: li sospende l'annullamento (ENG-38). */
	private final List<StatModifier> ownEffectBonuses = new ArrayList<>();
	private final List<StatModifier> modifiers = new ArrayList<>();
	/** Ferite non ancora riferite all'effetto, nell'ordine in cui sono avvenute (deciso il 18 novembre). */
	private final List<Wound> unreportedWounds = new ArrayList<>();
	/** Penalità non ancora controllate, nell'ordine in cui sono arrivate (Remnants of Reality, ENG-37). */
	private final List<Penalty> unreportedPenalties = new ArrayList<>();
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
	/** Danni prevenuti dal primo danno sopra 0 (SCP-500, ENG-40); 0 se non c'è protezione. */
	private int damageWard;
	/** Carte che hanno dato la protezione dal primo danno, per il log. */
	private final List<String> damageWardCards = new ArrayList<>();

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
	 * Tutti gli effetti che il motore chiama per questa creatura (ENG-34, deciso il 18 novembre): prima i suoi
	 * ({@link #ownEffects()}), poi quelli collegati da altre carte, nell'ordine in cui sono arrivati.
	 *
	 * @return gli effetti, anche nessuno
	 */
	public List<CreatureEffect> effects() {
		List<CreatureEffect> effects = ownEffects();
		effects.addAll(attachedEffects);
		return effects;
	}

	/**
	 * Gli effetti che sono della creatura (ENG-38, deciso il 19 novembre): il suo e quello in prestito, se c'è. Sono
	 * quelli che un annullamento spegne e che Baku divora; gli effetti collegati da una Spell non ne fanno parte.
	 *
	 * @return il suo effetto e quello in prestito, o nessuno se l'effetto è annullato
	 */
	public List<CreatureEffect> ownEffects() {
		List<CreatureEffect> effects = new ArrayList<>();
		if (cancelledUntil == null) {
			effect().ifPresent(effects::add);
			if (borrowedEffect != null) {
				effects.add(borrowedEffect);
			}
		}
		return effects;
	}

	/**
	 * Effetto preso in prestito da un'altra creatura: rubato (SCP-2521) o copiato (Kitsune).
	 *
	 * @return l'effetto in prestito, anche se annullato, o vuoto
	 */
	public Optional<CreatureEffect> borrowedEffect() {
		return Optional.ofNullable(borrowedEffect);
	}

	/**
	 * Prende in prestito l'effetto di un'altra creatura (ENG-38, deciso il 19 novembre): una nuova istanza, che il
	 * motore chiama dopo il suo effetto come se fosse suo. Ce n'è uno solo alla volta, così un effetto in prestito non
	 * ne prende un altro (SCP-2521 contro SCP-2521, Kitsune che copia SCP-2521).
	 *
	 * @param borrowed nuova istanza dell'effetto, solo per questa creatura
	 * @param duration {@code whileInPlay} (SCP-2521) o {@code untilCombatEnd} (Kitsune: si toglie quando l'avversaria
	 *                 esce)
	 * @return {@code false} se ha già un effetto in prestito: allora non cambia niente
	 * @throws IllegalArgumentException se la durata è a round
	 */
	public boolean borrowEffect(CreatureEffect borrowed, ModifierDuration duration) {
		Objects.requireNonNull(borrowed, "borrowed");
		ModifierDuration.Kind until = withoutRounds(duration);
		if (borrowedEffect != null) {
			return false;
		}
		borrowedEffect = borrowed;
		borrowedUntil = until;
		return true;
	}

	/**
	 * Indica se l'effetto della creatura è annullato.
	 *
	 * @return {@code true} se nessuno dei suoi effetti scatta
	 */
	public boolean isEffectCancelled() {
		return cancelledUntil != null;
	}

	/**
	 * Annulla l'effetto della creatura e quello in prestito (ENG-38, regola degli effetti annullati, deciso il 19
	 * novembre): non scattano più, e i bonus ATK che si sono dati sono sospesi per la stessa durata; la DEF già
	 * guadagnata resta. Gli effetti collegati da una Spell non si annullano. Un annullamento per tutta la permanenza
	 * vince su uno fino a fine scontro.
	 *
	 * @param duration {@code whileInPlay} (Il Predicatore sotto la Pioggia, SCP-2521) o {@code untilCombatEnd} (Baku:
	 *                 finché Baku è in campo)
	 * @return {@code true} se l'annullamento è nuovo o dura di più; {@code false} se c'era già
	 * @throws IllegalArgumentException se la durata è a round
	 */
	public boolean cancelEffect(ModifierDuration duration) {
		ModifierDuration.Kind until = withoutRounds(duration);
		if (cancelledUntil == ModifierDuration.Kind.WHILE_IN_PLAY || cancelledUntil == until) {
			return false;
		}
		cancelledUntil = until;
		for (StatModifier bonus : ownEffectBonuses) {
			bonus.suspend(duration);
		}
		return true;
	}

	/**
	 * Ricorda i bonus ATK che un suo effetto le ha appena dato (ENG-38): i modificatori di ATK positivi con origine
	 * {@code EFFECT} e il suo lato arrivati dopo {@code before}. Lo chiama il motore intorno a ogni chiamata dei suoi
	 * effetti, così l'annullamento sospende solo quelli (non, per esempio, il +1 ATK di SCP-2317 a un'alleata).
	 *
	 * @param before i modificatori prima della chiamata
	 */
	public void recordOwnEffectBonuses(List<StatModifier> before) {
		for (StatModifier modifier : modifiers) {
			if (modifier.source() == ModifierSource.EFFECT && modifier.side() == side
					&& modifier.stat() == Stat.ATTACK && modifier.amount() > 0 && !containsSame(before, modifier)
					&& !containsSame(ownEffectBonuses, modifier)) {
				ownEffectBonuses.add(modifier);
			}
		}
	}

	/**
	 * Indica se gli effetti delle creature avversarie non la colpiscono (Il Custode delle Porte Incrociate, ENG-38).
	 *
	 * @return {@code true} se uno dei suoi effetti la rende immune
	 */
	public boolean isImmuneToEnemyCreatureEffects() {
		return ownEffects().stream().anyMatch(CreatureEffect::immuneToEnemyCreatureEffects);
	}

	private static boolean containsSame(List<StatModifier> list, StatModifier modifier) {
		return list.stream().anyMatch(item -> item == modifier);
	}

	private static ModifierDuration.Kind withoutRounds(ModifierDuration duration) {
		if (duration.kind() == ModifierDuration.Kind.ROUNDS) {
			throw new IllegalArgumentException("Serve una durata finché resta in campo o fino a fine scontro: "
					+ duration);
		}
		return duration.kind();
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
	 * Aggiunge una protezione dal primo danno (SCP-500, ENG-40, deciso il 20 novembre): il primo danno sopra 0 che la
	 * creatura subisce, da un attacco o da un effetto, perde questi punti. Più protezioni si sommano sullo stesso
	 * primo danno. Non è un modificatore né l'effetto della creatura: annullamenti e rimozioni di bonus non la toccano,
	 * e se ne va solo con la creatura.
	 *
	 * @param card   nome della carta che la dà, per il log
	 * @param amount danni prevenuti, almeno 1
	 * @throws IllegalArgumentException se {@code amount} è minore di 1
	 */
	public void addDamageWard(String card, int amount) {
		Objects.requireNonNull(card, "card");
		if (amount < 1) {
			throw new IllegalArgumentException("Protezione non positiva: " + amount);
		}
		damageWard += amount;
		damageWardCards.add(card);
	}

	/**
	 * Danni che la protezione dal primo danno toglierà ancora (SCP-500, ENG-40).
	 *
	 * @return la protezione, 0 se non ce n'è o è già stata usata
	 */
	public int damageWard() {
		return damageWard;
	}

	/**
	 * Carte che hanno dato la protezione dal primo danno ancora da usare, nell'ordine in cui sono arrivate.
	 *
	 * @return copia non modificabile, vuota se non c'è protezione
	 */
	public List<String> damageWardCards() {
		return List.copyOf(damageWardCards);
	}

	/**
	 * Usa la protezione dal primo danno (SCP-500, ENG-40): un danno sopra 0 la consuma tutta, anche se è più piccolo
	 * (D-33); un danno da 0 non la tocca.
	 *
	 * @param damage danno in arrivo, almeno 0
	 * @return il danno che resta, almeno 0
	 */
	public int useDamageWard(int damage) {
		if (damage <= 0 || damageWard == 0) {
			return damage;
		}
		int remaining = Math.max(0, damage - damageWard);
		damageWard = 0;
		damageWardCards.clear();
		return remaining;
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
	 * Toglie i bonus temporanei (The Undying Blossom, ENG-37, deciso il 18 novembre): i modificatori di ATK positivi e
	 * gli scudi con una durata a round o fino a fine scontro. Restano malus, sigilli, congelamenti e i bonus che
	 * durano finché la creatura resta in campo, come le Spell (D-12).
	 *
	 * @return {@code true} se ha tolto almeno un modificatore
	 */
	public boolean removeTemporaryBonuses() {
		return modifiers.removeIf(modifier -> modifier.isBonus()
				&& modifier.durationKind() != ModifierDuration.Kind.WHILE_IN_PLAY);
	}

	/**
	 * Toglie i modificatori temporanei (The Null Witness, ENG-36, deciso il 18 novembre): tutti quelli con una durata
	 * a round o fino a fine scontro, di ogni segno (bonus, scudi, malus, sigilli, congelamenti). Restano quelli che
	 * durano finché la creatura resta in campo, come le Spell (D-12) e il Terreno. Dello scudo sparisce solo la parte
	 * non usata, quindi non uccide mai.
	 *
	 * @return {@code true} se ha tolto almeno un modificatore
	 */
	public boolean removeTemporaryModifiers() {
		return modifiers.removeIf(modifier -> modifier.durationKind() != ModifierDuration.Kind.WHILE_IN_PLAY);
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
	 * Aggiunge un modificatore. Un malus ATK causato dall'avversario è una penalità e resta in coda finché il motore
	 * la controlla (ENG-37).
	 *
	 * @param modifier il modificatore
	 */
	public void addModifier(StatModifier modifier) {
		modifiers.add(modifier);
		if (isEnemyAttackPenalty(modifier)) {
			unreportedPenalties.add(new Penalty(0, modifier));
		}
	}

	/**
	 * Registra una penalità DEF: DEF persa per un effetto, una Spell, una Hidden, un danno in attesa o un Terreno
	 * dell'avversario (la chiama {@code Penalties}, ENG-37). Il danno l'ha già applicato chi chiama.
	 *
	 * @param lost DEF persa davvero; con 0 non c'è penalità
	 */
	public void recordDefensePenalty(int lost) {
		if (lost > 0) {
			unreportedPenalties.add(new Penalty(lost, null));
		}
	}

	/**
	 * Indica se ci sono penalità non ancora controllate.
	 *
	 * @return {@code true} se ce n'è almeno una
	 */
	public boolean hasUnreportedPenalties() {
		return !unreportedPenalties.isEmpty();
	}

	/**
	 * Prende le penalità non ancora controllate, nell'ordine in cui sono arrivate, e le toglie dalla coda (ENG-37).
	 *
	 * @return le penalità, anche nessuna
	 */
	public List<Penalty> takeUnreportedPenalties() {
		List<Penalty> penalties = List.copyOf(unreportedPenalties);
		unreportedPenalties.clear();
		return penalties;
	}

	/**
	 * Malus ATK dell'avversario ancora attivi, nell'ordine in cui sono arrivati (Remnants of Reality).
	 *
	 * @return i malus, anche nessuno
	 */
	public List<StatModifier> enemyAttackPenalties() {
		return modifiers.stream().filter(this::isEnemyAttackPenalty).toList();
	}

	/**
	 * Toglie punti a un malus ATK ancora attivo (Remnants of Reality, ENG-37): il malus resta con gli altri punti, o
	 * sparisce se non ne restano.
	 *
	 * @param malus  il malus
	 * @param points punti da togliere, almeno 0
	 * @return i punti tolti davvero: 0 se il malus non c'è più
	 */
	public int cancelAttackPenalty(StatModifier malus, int points) {
		int index = modifiers.indexOf(malus);
		if (index < 0 || points <= 0) {
			return 0;
		}
		int cancelled = Math.min(points, -malus.amount());
		int remaining = malus.amount() + cancelled;
		if (remaining == 0) {
			modifiers.remove(index);
		} else {
			modifiers.set(index, malus.withAttack(remaining));
		}
		return cancelled;
	}

	/**
	 * Ridà DEF persa per una penalità (Remnants of Reality, ENG-37, deciso il 18 novembre): DEF permanente in più,
	 * anche se la creatura ha DEF 0 o meno, quindi può evitare la morte. Il tetto ("senza superare i valori di prima")
	 * lo controlla chi chiama, che sa quanta DEF ha tolto la penalità.
	 *
	 * @param amount DEF da ridare, almeno 0
	 * @throws IllegalArgumentException se {@code amount} è negativo
	 */
	public void restoreDefense(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("DEF da ridare negativa: " + amount);
		}
		permanentDefense += amount;
	}

	private boolean isEnemyAttackPenalty(StatModifier modifier) {
		return modifier.stat() == Stat.ATTACK && modifier.amount() < 0 && modifier.side() != side;
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
		// ENG-38: finisce l'annullamento di Baku (le sospensioni dei bonus sono finite qui sopra) e la copia di Kitsune
		if (cancelledUntil == ModifierDuration.Kind.UNTIL_COMBAT_END) {
			cancelledUntil = null;
		}
		if (borrowedUntil == ModifierDuration.Kind.UNTIL_COMBAT_END) {
			borrowedEffect = null;
			borrowedUntil = null;
		}
		ownEffectBonuses.removeIf(bonus -> !containsSame(modifiers, bonus));
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
