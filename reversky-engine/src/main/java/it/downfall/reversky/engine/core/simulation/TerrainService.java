package it.downfall.reversky.engine.core.simulation;

import java.util.Optional;
import java.util.function.Consumer;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.TerrainEffect;
import it.downfall.reversky.engine.core.effect.Terrains;
import it.downfall.reversky.engine.core.log.MatchLog;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.ModifierDuration;
import it.downfall.reversky.engine.core.model.ModifierSource;
import it.downfall.reversky.engine.core.model.PlacedTerrain;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.model.StatModifier;

/**
 * Terreni (docs/carte, "Terreno"): in gioco c'è un solo Terreno, quello pescato dopo sostituisce il precedente di
 * chiunque sia (D-19), e il suo bonus base va a tutte le creature in campo della sua fazione, comprese quelle
 * dell'avversario (D-17).
 * <ul>
 * <li>Il bonus si riceve una volta per Terreno: subito, se la creatura è in campo quando il Terreno arriva, oppure
 * all'inizio del suo primo scontro (passo 0, dopo i modificatori in attesa). Una creatura con due fazioni lo riceve
 * una volta sola.</li>
 * <li>ATK e DEF durano finché il Terreno resta in gioco; la DEF è uno scudo (D-35) che assorbe i danni prima degli
 * altri scudi "finché resta in campo".</li>
 * <li>Quando il Terreno esce si tolgono i suoi bonus: l'ATK subito, dello scudo solo la parte non consumata (D-19),
 * quindi l'uscita non uccide mai.</li>
 * </ul>
 * Gli effetti secondari (ENG-27) stanno nell'effetto del Terreno in gioco, una nuova istanza per ogni messa in gioco
 * ({@link PlacedTerrain#effect()}): qui scattano quelli "a inizio scontro"; le ferite e le penalità passano da
 * {@link CreatureEffectService} e da {@code Penalties}.
 */
public final class TerrainService {

	private final EffectRegistry effects;
	private final CreatureEffectService creatureEffects;

	/**
	 * Crea il servizio. Non conserva stato tra le partite.
	 *
	 * @param effects registro degli effetti
	 */
	public TerrainService(EffectRegistry effects) {
		this.effects = effects;
		this.creatureEffects = new CreatureEffectService(new ItemService(effects));
	}

	/**
	 * Mette in gioco un Terreno appena pescato: toglie i bonus di quello che sostituisce e dà il suo alle creature in
	 * campo della sua fazione, prima lo sfidante. Prima però una creatura in campo lo può inghiottire (The Hollow Wyrm,
	 * ENG-36): allora non si piazza e non sostituisce niente.
	 *
	 * @param state stato della partita
	 * @param owner giocatore che l'ha pescato
	 * @param card  carta del Terreno
	 */
	public void place(MatchState state, PlayerState owner, CardDefinition card) {
		if (creatureEffects.terrainArriving(state, card)) {
			return;
		}
		Optional<PlacedTerrain> replaced = state.placeTerrain(card, owner.side(),
				effects.createTerrain(card.effectCode()));
		state.log().terrainPlaced(owner.side(), card, replaced.map(PlacedTerrain::card).orElse(null));
		replaced.ifPresent(old -> Terrains.loseBonuses(state, old));
		forEachCreature(state, creature -> applyBonus(state, creature));
	}

	/**
	 * Dà il bonus del Terreno in gioco a una creatura della sua fazione che non l'ha ancora ricevuto (passo 0).
	 *
	 * @param state    stato della partita
	 * @param creature creatura in campo
	 */
	public void applyBonus(MatchState state, CreatureInPlay creature) {
		Optional<PlacedTerrain> terrain = state.terrain();
		if (terrain.isEmpty() || creature.hasTerrainBonusFrom(terrain.get()) || !terrain.get().affects(creature)) {
			return;
		}
		PlacedTerrain placed = terrain.get();
		TerrainEffect effect = placed.effect();
		if (effect.attackBonus() > 0) {
			creature.addModifier(StatModifier.attack(ModifierSource.TERRAIN, placed.owner(), effect.attackBonus(),
					ModifierDuration.whileInPlay()));
		}
		if (effect.defenseBonus() > 0) {
			creature.addModifier(StatModifier.shield(ModifierSource.TERRAIN, placed.owner(), effect.defenseBonus(),
					ModifierDuration.whileInPlay()));
		}
		creature.recordTerrainBonus(placed);
		state.log().effectTriggered(creature.side(), creature.name(), describe(effect) + " from "
				+ placed.card().name() + " " + stats(creature));
	}

	/**
	 * Effetti secondari "a inizio scontro" (Devouring Abyss), alla fine del passo 0, dopo che si è fissata la DEF
	 * all'entrata: per ogni creatura viva della fazione del Terreno, prima lo sfidante; dopo ognuna si controllano le
	 * morti e, se una creatura muore, lo scontro finisce lì (deciso il 18 novembre).
	 *
	 * @param state  stato della partita, con due creature in campo
	 * @param deaths controllo delle morti
	 */
	public void startCombat(MatchState state, DeathResolver deaths) {
		Optional<PlacedTerrain> terrain = state.terrain();
		if (terrain.isEmpty()) {
			return;
		}
		for (Side side : Side.values()) {
			CreatureInPlay creature = state.player(side).creature().orElse(null);
			if (creature != null && !creature.isDead() && terrain.get().affects(creature)) {
				terrain.get().effect().onCombatStart(state, terrain.get(), creature);
				if (deaths.resolve(state)) {
					return;
				}
			}
		}
	}

	private static void forEachCreature(MatchState state, Consumer<CreatureInPlay> action) {
		for (Side side : Side.values()) {
			state.player(side).creature().ifPresent(action);
		}
	}

	private static String describe(TerrainEffect effect) {
		StringBuilder text = new StringBuilder();
		if (effect.attackBonus() > 0) {
			text.append(MatchLog.signed(effect.attackBonus())).append(" ATK");
		}
		if (effect.defenseBonus() > 0) {
			text.append(text.isEmpty() ? "" : ", ").append(MatchLog.signed(effect.defenseBonus())).append(" DEF");
		}
		return text.toString();
	}

	private static String stats(CreatureInPlay creature) {
		return "(ATK " + MatchLog.number(creature.attack()) + ", DEF " + MatchLog.number(creature.defense()) + ")";
	}
}
