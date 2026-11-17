package it.downfall.reversky.engine.support;

import java.util.List;
import java.util.stream.Stream;

import it.downfall.reversky.engine.core.effect.EffectContext;
import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.effect.HiddenEffect;
import it.downfall.reversky.engine.core.effect.ItemEffect;
import it.downfall.reversky.engine.core.effect.SpellEffect;
import it.downfall.reversky.engine.core.effect.TerrainEffect;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.SimulationInput;

/**
 * Registri degli effetti per i test che usano Spell, Hidden, Terreni od Oggetti il cui effetto vero non esiste ancora:
 * quei codici sono registrati con un effetto finto che non fa niente, così l'input supera il controllo
 * {@code UNKNOWN_EFFECT} e la carta si gioca senza effetto.
 */
public final class TestEffects {

	private TestEffects() {
	}

	/**
	 * Registro con un effetto finto per ogni codice delle carte non creatura dei due mazzi.
	 *
	 * @param input input della simulazione
	 * @return un nuovo registro
	 */
	public static EffectRegistry stubsFor(SimulationInput input) {
		return stubsFor(Stream.concat(input.challenger().deck().stream(), input.opponent().deck().stream()).toList());
	}

	/**
	 * Registro con un effetto finto per ogni codice delle carte non creatura date. Un codice ripetuto si registra una
	 * volta sola; le creature si ignorano.
	 *
	 * @param cards carte, anche ripetute
	 * @return un nuovo registro
	 */
	public static EffectRegistry stubsFor(List<CardDefinition> cards) {
		return addStubs(new EffectRegistry(), cards);
	}

	/**
	 * Registro del gioco ({@link EffectRegistry#withAllEffects()}) più un effetto finto per ogni codice delle carte
	 * non creatura dei due mazzi che il gioco non ha ancora: gli effetti già implementati sono quelli veri.
	 *
	 * @param input input della simulazione
	 * @return un nuovo registro
	 */
	public static EffectRegistry gameWithStubsFor(SimulationInput input) {
		return addStubs(EffectRegistry.withAllEffects(),
				Stream.concat(input.challenger().deck().stream(), input.opponent().deck().stream()).toList());
	}

	private static EffectRegistry addStubs(EffectRegistry registry, List<CardDefinition> cards) {
		cards.stream()
				.filter(card -> card.effectCode() != null && registry.appliesTo(card.effectCode()).isEmpty())
				.forEach(card -> register(registry, card));
		return registry;
	}

	private static void register(EffectRegistry registry, CardDefinition card) {
		String code = card.effectCode();
		switch (card.type()) {
			case SPELL -> registry.registerSpell(code, () -> new StubSpell(code));
			case HIDDEN -> registry.registerHidden(code, () -> new StubHidden(code));
			case TERRAIN -> registry.registerTerrain(code, () -> new StubTerrain(code));
			case ITEM -> registry.registerItem(code, () -> new StubItem(code));
			case CREATURE -> {
				// gli effetti delle creature arrivano con ENG-23: il registro non li conosce ancora
			}
		}
	}

	private record StubSpell(String code) implements SpellEffect {

		@Override
		public void resolve(EffectContext ctx) {
			// effetto finto: non fa niente
		}
	}

	private record StubHidden(String code) implements HiddenEffect {

		@Override
		public void trigger(EffectContext ctx) {
			// effetto finto: non fa niente
		}
	}

	private record StubTerrain(String code) implements TerrainEffect {

		@Override
		public int attackBonus() {
			return 0;
		}

		@Override
		public int defenseBonus() {
			return 0;
		}
	}

	private record StubItem(String code) implements ItemEffect {
	}
}
