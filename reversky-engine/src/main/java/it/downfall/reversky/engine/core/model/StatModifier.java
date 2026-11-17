package it.downfall.reversky.engine.core.model;

/**
 * Modificatore di una creatura in campo (D-35): un bonus o malus di ATK, oppure uno scudo di DEF temporanea.
 * <p>
 * Ogni modificatore riporta l'origine ({@link ModifierSource}) e il lato che l'ha causato, così gli effetti possono
 * distinguerlo, sospenderlo o toglierlo. Ha una durata ({@link ModifierDuration}) e si può sospendere per intero:
 * mentre è sospeso non conta (né l'ATK né lo scudo). Sospendere solo una parte di un modificatore (Static
 * Interference, Remnants of Reality) è compito di quegli effetti.
 * <p>
 * Lo stato cambia solo tramite {@link CreatureInPlay}, che conosce l'ordine degli scudi e la fine di round e scontri.
 */
public final class StatModifier {

	private static final int RANK_ROUNDS = 0;
	private static final int RANK_COMBAT = 1;
	private static final int RANK_TERRAIN = 2;
	private static final int RANK_IN_PLAY = 3;

	private final ModifierSource source;
	private final Side side;
	private final Stat stat;
	private final int amount;
	private final ModifierDuration.Kind durationKind;
	private int remainingRounds;
	private int remainingShield;
	private ModifierDuration suspension;

	private StatModifier(ModifierSource source, Side side, Stat stat, int amount, ModifierDuration duration) {
		this.source = source;
		this.side = side;
		this.stat = stat;
		this.amount = amount;
		this.durationKind = duration.kind();
		this.remainingRounds = duration.rounds();
		this.remainingShield = stat == Stat.DEFENSE_SHIELD ? amount : 0;
	}

	/**
	 * Bonus o malus di ATK.
	 *
	 * @param source   origine
	 * @param side     lato che l'ha causato
	 * @param amount   valore da sommare all'attacco; negativo per un malus
	 * @param duration durata
	 * @return il modificatore
	 */
	public static StatModifier attack(ModifierSource source, Side side, int amount, ModifierDuration duration) {
		return new StatModifier(source, side, Stat.ATTACK, amount, duration);
	}

	/**
	 * Scudo di DEF temporanea (D-35): assorbe i danni prima della DEF; alla scadenza sparisce solo la parte non
	 * usata, quindi la scadenza non uccide mai.
	 *
	 * @param source   origine
	 * @param side     lato che l'ha causato
	 * @param amount   punti di DEF dello scudo, almeno 1
	 * @param duration durata
	 * @return il modificatore
	 * @throws IllegalArgumentException se {@code amount} è minore di 1
	 */
	public static StatModifier shield(ModifierSource source, Side side, int amount, ModifierDuration duration) {
		if (amount < 1) {
			throw new IllegalArgumentException("Uno scudo vale almeno 1 DEF: " + amount);
		}
		return new StatModifier(source, side, Stat.DEFENSE_SHIELD, amount, duration);
	}

	/**
	 * Origine del modificatore.
	 *
	 * @return l'origine
	 */
	public ModifierSource source() {
		return source;
	}

	/**
	 * Lato che ha causato il modificatore (non per forza il proprietario della creatura).
	 *
	 * @return il lato
	 */
	public Side side() {
		return side;
	}

	/**
	 * Statistica su cui agisce.
	 *
	 * @return la statistica
	 */
	public Stat stat() {
		return stat;
	}

	/**
	 * Valore iniziale: ATK sommato all'attacco, o punti di DEF dello scudo appena creato.
	 *
	 * @return il valore iniziale
	 */
	public int amount() {
		return amount;
	}

	/**
	 * Tipo di durata.
	 *
	 * @return il tipo di durata
	 */
	public ModifierDuration.Kind durationKind() {
		return durationKind;
	}

	/**
	 * Round rimasti per la durata {@code ROUNDS}.
	 *
	 * @return i round rimasti; 0 per gli altri tipi
	 */
	public int remainingRounds() {
		return remainingRounds;
	}

	/**
	 * Indica se il modificatore è sospeso.
	 *
	 * @return {@code true} se ora non conta
	 */
	public boolean isSuspended() {
		return suspension != null;
	}

	/**
	 * Contributo all'attacco.
	 *
	 * @return {@link #amount()} per un modificatore di ATK attivo; 0 se è sospeso o è uno scudo
	 */
	public int attackBonus() {
		return stat == Stat.ATTACK && !isSuspended() ? amount : 0;
	}

	/**
	 * DEF che lo scudo può ancora assorbire.
	 *
	 * @return la parte non usata di uno scudo attivo; 0 se è sospeso o è un modificatore di ATK
	 */
	public int shieldValue() {
		return stat == Stat.DEFENSE_SHIELD && !isSuspended() ? remainingShield : 0;
	}

	/**
	 * Sospende il modificatore: non conta finché la sospensione dura. Una nuova sospensione sostituisce la
	 * precedente.
	 *
	 * @param duration durata della sospensione
	 */
	public void suspend(ModifierDuration duration) {
		this.suspension = duration;
	}

	/** Toglie la sospensione prima della sua fine (es. la creatura che la causava lascia il campo). */
	public void resume() {
		this.suspension = null;
	}

	/**
	 * Assorbe un danno con lo scudo.
	 *
	 * @param damage danno in arrivo, almeno 0
	 * @return il danno che resta dopo lo scudo
	 */
	int absorb(int damage) {
		int absorbed = Math.min(shieldValue(), damage);
		remainingShield -= absorbed;
		return damage - absorbed;
	}

	/**
	 * Uno scudo consumato del tutto non serve più.
	 *
	 * @return {@code true} se è uno scudo senza DEF rimasta
	 */
	boolean isExhausted() {
		return stat == Stat.DEFENSE_SHIELD && remainingShield == 0;
	}

	/**
	 * Fine di un round risolto: scendono di 1 i round della durata e della sospensione.
	 *
	 * @return {@code true} se il modificatore è scaduto e va tolto
	 */
	boolean endRound() {
		if (suspension != null && suspension.kind() == ModifierDuration.Kind.ROUNDS) {
			suspension = suspension.rounds() == 1 ? null : ModifierDuration.rounds(suspension.rounds() - 1);
		}
		if (durationKind != ModifierDuration.Kind.ROUNDS) {
			return false;
		}
		remainingRounds--;
		return remainingRounds == 0;
	}

	/**
	 * Fine dello scontro: finiscono le sospensioni fino a fine scontro.
	 *
	 * @return {@code true} se il modificatore dura fino a fine scontro e va tolto
	 */
	boolean endCombat() {
		if (suspension != null && suspension.kind() == ModifierDuration.Kind.UNTIL_COMBAT_END) {
			suspension = null;
		}
		return durationKind == ModifierDuration.Kind.UNTIL_COMBAT_END;
	}

	/**
	 * Ordine in cui gli scudi assorbono i danni: prima quelli che scadono prima (a round, con meno round rimasti),
	 * poi quelli fino a fine scontro, poi quello del Terreno (sparisce quando il Terreno esce, che può succedere in
	 * ogni momento: deciso il 17 novembre), poi gli altri finché la creatura resta in campo.
	 *
	 * @return chiave di ordinamento, più bassa = assorbe prima
	 */
	long expiryRank() {
		return switch (durationKind) {
			case ROUNDS -> ((long) RANK_ROUNDS << 32) + remainingRounds;
			case UNTIL_COMBAT_END -> (long) RANK_COMBAT << 32;
			case WHILE_IN_PLAY -> (long) (source == ModifierSource.TERRAIN ? RANK_TERRAIN : RANK_IN_PLAY) << 32;
		};
	}
}
