package it.downfall.reversky.engine.core.model;

import java.util.Objects;

import it.downfall.reversky.engine.core.effect.CreatureEffect;
import it.downfall.reversky.engine.core.effect.Penalties;

/**
 * Modificatore in attesa (D-34): un bonus, un malus o un danno da effetto che aspetta la prossima creatura di un
 * giocatore. Lo usano le Spell {@code SHIELD} e {@code BURN} sulla propria creatura e le Spell {@code DAMAGE} senza
 * bersaglio sulla creatura avversaria (D-10, D-11, D-13); con ENG-23 anche gli effetti che preparano la prossima
 * creatura.
 * <p>
 * Si applica all'inizio del primo scontro della creatura, dopo il bonus DEF di fazione (docs/effetti, passo 0). ATK e
 * scudo durano finché la creatura resta in campo (D-12); il danno è una perdita permanente di DEF (D-49). Può anche
 * collegare un effetto alla creatura che lo riceve (SCP-109, ENG-34): l'istanza è una sola, quindi il modificatore
 * deve raggiungere una sola creatura.
 *
 * @param card     nome della carta che l'ha creato, per il log
 * @param source   origine dei modificatori che crea (D-35)
 * @param side     lato che l'ha causato (non per forza il proprietario della creatura che lo riceve)
 * @param attack   ATK da sommare; negativo per un malus, 0 se non tocca l'ATK
 * @param shield   DEF in più, come scudo (D-35); 0 se non dà DEF
 * @param damage   danno da effetto alla DEF; 0 se non fa danni
 * @param attached effetto da collegare alla creatura che lo riceve, o {@code null}
 */
public record PendingModifier(String card, ModifierSource source, Side side, int attack, int shield, int damage,
		CreatureEffect attached) {

	/**
	 * Controlla i valori.
	 *
	 * @throws IllegalArgumentException se scudo o danno sono negativi, o se non cambia niente
	 * @throws NullPointerException     se manca la carta, l'origine o il lato
	 */
	public PendingModifier {
		Objects.requireNonNull(card, "card");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(side, "side");
		if (shield < 0 || damage < 0) {
			throw new IllegalArgumentException("Scudo e danno non negativi: " + shield + ", " + damage);
		}
		if (attack == 0 && shield == 0 && damage == 0 && attached == null) {
			throw new IllegalArgumentException("Modificatore in attesa vuoto: " + card);
		}
	}

	/**
	 * Crea un modificatore senza effetto collegato.
	 *
	 * @param card   nome della carta che l'ha creato, per il log
	 * @param source origine dei modificatori che crea (D-35)
	 * @param side   lato che l'ha causato
	 * @param attack ATK da sommare; negativo per un malus, 0 se non tocca l'ATK
	 * @param shield DEF in più, come scudo (D-35); 0 se non dà DEF
	 * @param damage danno da effetto alla DEF; 0 se non fa danni
	 * @throws IllegalArgumentException se scudo o danno sono negativi, o se non cambia niente
	 */
	public PendingModifier(String card, ModifierSource source, Side side, int attack, int shield, int damage) {
		this(card, source, side, attack, shield, damage, null);
	}

	/**
	 * Applica il modificatore a una creatura: prima l'ATK e lo scudo, poi il danno da effetto, che lo scudo appena
	 * dato assorbe per primo (D-35). Il danno passa dalle riduzioni del Terreno in gioco ({@link Penalties}, ENG-27).
	 * Le morti le controlla chi lo chiama (D-08).
	 *
	 * @param state    partita, per il Terreno in gioco
	 * @param creature creatura che lo riceve
	 */
	public void applyTo(MatchState state, CreatureInPlay creature) {
		Penalties.apply(state, creature, this);
	}
}
