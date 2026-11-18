package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;
import java.util.function.Predicate;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.ItemEffect;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.RoundStartShield;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Oggetti (docs/carte, "Oggetto"): pescato, l'Oggetto resta accanto al mazzo e non si può distruggere né scartare;
 * scatta una volta, poi si consuma (D-47). Ogni metodo è un momento in cui un Oggetto può scattare; se il proprietario
 * ha un Oggetto adatto, lo consuma, lo registra con {@code ITEM_CONSUMED} e dice a chi chiama cosa è successo.
 * <ul>
 * <li>Badge di Uscita Negata: nel controllo delle morti, prima di togliere la creatura (D-36).</li>
 * <li>Bussola dei Sentieri Falsi: all'inizio dello scontro, dopo il Terreno (D-48); lo scudo arriva a inizio
 * round.</li>
 * <li>Fascicolo Sigillato 0: quando una Spell o una Hidden avversaria colpirebbe la creatura del proprietario.</li>
 * <li>Campanello del Torii: quando una Hidden avversaria starebbe per attivarsi.</li>
 * </ul>
 */
public final class ItemService {

	private final EffectRegistry effects;

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti
	 */
	public ItemService(EffectRegistry effects) {
		this.effects = effects;
	}

	/**
	 * Mette accanto al mazzo un Oggetto appena pescato.
	 *
	 * @param state stato della partita
	 * @param owner giocatore che l'ha pescato
	 * @param item  carta dell'Oggetto
	 */
	public void place(MatchState state, PlayerState owner, CardDefinition item) {
		owner.placeItem(item);
		state.log().itemPlaced(owner.side(), item);
	}

	/**
	 * Controllo delle morti: una creatura morta del proprietario di un Badge resta in vita (D-36).
	 *
	 * @param state    stato della partita
	 * @param creature creatura con DEF 0 o meno
	 * @return {@code true} se un Oggetto l'ha salvata
	 */
	public boolean saveFromDeath(MatchState state, CreatureInPlay creature) {
		PlayerState owner = state.player(creature.side());
		Optional<Found> badge = find(owner, effect -> effect.survivalDefense() > 0);
		if (badge.isEmpty()) {
			return false;
		}
		int defense = badge.get().effect().survivalDefense();
		creature.surviveWith(defense);
		consume(state, owner, badge.get().card(), "stays at " + defense + " DEF", creature.name());
		return true;
	}

	/**
	 * Inizio dello scontro (passo 0, dopo il Terreno): una Bussola del proprietario programma gli scudi a inizio round
	 * sulla sua creatura.
	 *
	 * @param state    stato della partita
	 * @param creature creatura in campo
	 */
	public void startCombat(MatchState state, CreatureInPlay creature) {
		PlayerState owner = state.player(creature.side());
		Optional<Found> compass = find(owner, effect -> effect.roundStartShield() > 0);
		if (compass.isEmpty()) {
			return;
		}
		ItemEffect effect = compass.get().effect();
		creature.scheduleRoundStartShield(new RoundStartShield(ModifierSource.ITEM, owner.side(),
				effect.roundStartShield(), effect.roundStartShieldRounds()));
		consume(state, owner, compass.get().card(), MatchLog.signed(effect.roundStartShield())
				+ " DEF shield at the start of each of the first " + effect.roundStartShieldRounds() + " rounds",
				creature.name());
	}

	/**
	 * Inizio di un round (passo 1): la creatura riceve lo scudo programmato, se ce n'è uno.
	 *
	 * @param state    stato della partita
	 * @param creature creatura in campo
	 */
	public void startRound(MatchState state, CreatureInPlay creature) {
		Optional<StatModifier> shield = creature.startRound();
		shield.ifPresent(given -> state.log().effectTriggered(creature.side(), creature.name(),
				MatchLog.signed(given.amount()) + " DEF shield this round (DEF " + MatchLog.number(creature.defense())
						+ ")"));
	}

	/**
	 * Una Spell o una Hidden avversaria sta per colpire la creatura del proprietario (in campo, o la prossima se il
	 * colpo era in attesa): un Fascicolo Sigillato 0 la annulla del tutto.
	 *
	 * @param state    stato della partita
	 * @param target   giocatore la cui creatura sarebbe colpita
	 * @param source   carta avversaria annullata
	 * @param creature nome della creatura protetta
	 * @return {@code true} se l'effetto è annullato
	 */
	public boolean cancelEnemyEffect(MatchState state, PlayerState target, String source, String creature) {
		return cancel(state, target, effect -> effect.cancelsEnemyEffect(), source, creature);
	}

	/**
	 * Una Hidden avversaria sta per attivarsi: un Campanello del Torii la annulla.
	 *
	 * @param state    stato della partita
	 * @param target   avversario del proprietario della Hidden
	 * @param hidden   Hidden annullata
	 * @return {@code true} se la Hidden è annullata
	 */
	public boolean cancelEnemyHidden(MatchState state, PlayerState target, CardDefinition hidden) {
		String creature = target.creature().map(CreatureInPlay::name).orElse(null);
		return cancel(state, target, effect -> effect.cancelsEnemyHidden(), hidden.name(), creature);
	}

	private boolean cancel(MatchState state, PlayerState owner, Predicate<ItemEffect> condition, String source,
			String creature) {
		Optional<Found> item = find(owner, condition);
		item.ifPresent(found -> consume(state, owner, found.card(), "cancels " + source, creature));
		return item.isPresent();
	}

	private Optional<Found> find(PlayerState owner, Predicate<ItemEffect> condition) {
		for (CardDefinition item : owner.items()) {
			ItemEffect effect = effects.createItem(item.effectCode());
			if (condition.test(effect)) {
				return Optional.of(new Found(item, effect));
			}
		}
		return Optional.empty();
	}

	private static void consume(MatchState state, PlayerState owner, CardDefinition item, String effect,
			String creature) {
		owner.consumeItem(item);
		state.log().itemConsumed(owner.side(), item, effect, creature);
	}

	/** Un Oggetto del proprietario e il suo effetto. */
	private record Found(CardDefinition card, ItemEffect effect) {
	}
}
