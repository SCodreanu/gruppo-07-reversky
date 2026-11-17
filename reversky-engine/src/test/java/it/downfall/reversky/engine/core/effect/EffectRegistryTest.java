package it.downfall.reversky.engine.core.effect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.downfall.reversky.engine.core.log.EventType;
import it.downfall.reversky.engine.core.log.MatchEvent;
import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardSubtype;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.support.TestCards;
import it.downfall.reversky.engine.support.TestRules;
import it.downfall.reversky.engine.support.TestStates;

/**
 * Test di {@link EffectRegistry} con tre effetti di prova, uno per Spell, Hidden e Terreno (ENG-22): gli effetti veri
 * arrivano con ENG-19, ENG-20 ed ENG-21 e usano lo stesso registro.
 */
class EffectRegistryTest {

	private static final CardDefinition DRAIN = TestCards.spell(900, "Test Drain", CardSubtype.DAMAGE, DrainSpell.CODE);

	private final EffectRegistry registry = new EffectRegistry()
			.registerSpell(DrainSpell.CODE, DrainSpell::new)
			.registerHidden(CountingHidden.CODE, CountingHidden::new)
			.registerTerrain(PlainTerrain.CODE, PlainTerrain::new);

	@Test
	@DisplayName("Ogni codice vale per il suo tipo di carta: TEST_DRAIN esiste per una Spell, non per un Terreno")
	void exists_codeOfItsCardTypeOnly() {
		assertThat(registry.exists(DrainSpell.CODE, CardType.SPELL)).isTrue();
		assertThat(registry.exists(DrainSpell.CODE, CardType.TERRAIN)).isFalse();
		assertThat(registry.exists("TEST_UNKNOWN", CardType.SPELL)).isFalse();
		assertThat(registry.appliesTo(PlainTerrain.CODE)).contains(CardType.TERRAIN);
		assertThat(registry.appliesTo("TEST_UNKNOWN")).isEmpty();
	}

	@Test
	@DisplayName("codes(): tutti i codici con il tipo di carta, nell'ordine di registrazione, non modificabili")
	void codes_inRegistrationOrder() {
		// Act
		Map<String, CardType> codes = registry.codes();

		// Assert
		assertThat(codes).containsExactly(
				Map.entry(DrainSpell.CODE, CardType.SPELL),
				Map.entry(CountingHidden.CODE, CardType.HIDDEN),
				Map.entry(PlainTerrain.CODE, CardType.TERRAIN));
		assertThatThrownBy(() -> codes.put("TEST_OTHER", CardType.SPELL))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	@DisplayName("Spell di prova: risolta con il contesto, toglie 2 punti vita all'avversario del proprietario")
	void createSpell_resolvesWithContext() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		SpellEffect spell = registry.createSpell(DrainSpell.CODE);

		// Act
		spell.resolve(new EffectContext(state, Side.CHALLENGER, DRAIN));

		// Assert
		assertThat(state.opponent().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS - 2);
		assertThat(state.challenger().lifePoints()).isEqualTo(TestRules.STARTING_LIFE_POINTS);
		assertThat(state.log().events()).extracting(MatchEvent::type).containsExactly(EventType.SPELL_RESOLVED);
	}

	@Test
	@DisplayName("Hidden di prova: ogni create dà una nuova istanza, quindi lo stato di un effetto non si condivide")
	void createHidden_newInstanceEachTime() {
		// Arrange
		MatchState state = TestStates.withDecks(List.of(), List.of());
		CardDefinition card = TestCards.hidden(901, "Test Counter", CardSubtype.SURVIVAL, CountingHidden.CODE);
		CountingHidden first = (CountingHidden) registry.createHidden(CountingHidden.CODE);
		CountingHidden second = (CountingHidden) registry.createHidden(CountingHidden.CODE);

		// Act
		first.trigger(new EffectContext(state, Side.OPPONENT, card));
		first.trigger(new EffectContext(state, Side.OPPONENT, card));

		// Assert
		assertThat(second).isNotSameAs(first);
		assertThat(first.triggers).isEqualTo(2);
		assertThat(second.triggers).isZero();
	}

	@Test
	@DisplayName("Terreno di prova: il registro restituisce il suo bonus base")
	void createTerrain_returnsBaseBonus() {
		// Act
		TerrainEffect terrain = registry.createTerrain(PlainTerrain.CODE);

		// Assert
		assertThat(terrain.code()).isEqualTo(PlainTerrain.CODE);
		assertThat(terrain.attackBonus()).isEqualTo(1);
		assertThat(terrain.defenseBonus()).isEqualTo(2);
	}

	@Test
	@DisplayName("Un Oggetto si registra e si crea come gli altri tipi")
	void registerItem_createsItem() {
		// Arrange
		EffectRegistry withItem = new EffectRegistry().registerItem(TestItem.CODE, TestItem::new);

		// Act + Assert
		assertThat(withItem.createItem(TestItem.CODE)).isInstanceOf(TestItem.class);
		assertThat(withItem.exists(TestItem.CODE, CardType.ITEM)).isTrue();
	}

	@Test
	@DisplayName("Creare un effetto con il tipo sbagliato o con un codice che non c'è è un bug del motore")
	void create_wrongTypeOrUnknownCode_throws() {
		assertThatThrownBy(() -> registry.createTerrain(DrainSpell.CODE))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.createSpell("TEST_UNKNOWN"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Un codice si registra una volta sola, anche per un altro tipo di carta; il codice non è vuoto")
	void register_duplicateOrBlankCode_throws() {
		assertThatThrownBy(() -> registry.registerTerrain(DrainSpell.CODE, PlainTerrain::new))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.registerSpell(" ", DrainSpell::new))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.registerSpell(null, DrainSpell::new))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Un effetto registrato con un codice diverso dal suo code() è un errore di registrazione")
	void create_codeMismatch_throws() {
		// Arrange
		EffectRegistry wrong = new EffectRegistry().registerSpell("TEST_OTHER", DrainSpell::new);

		// Act + Assert
		assertThatThrownBy(() -> wrong.createSpell("TEST_OTHER"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(DrainSpell.CODE);
	}

	@Test
	@DisplayName("Il registro del gioco crea ogni effetto registrato con il suo codice")
	void withAllEffects_everyCodeCreatesItsEffect() {
		// Arrange
		EffectRegistry game = EffectRegistry.withAllEffects();

		// Act + Assert
		game.codes().forEach((code, type) -> {
			CardEffect effect = switch (type) {
				case SPELL -> game.createSpell(code);
				case HIDDEN -> game.createHidden(code);
				case TERRAIN -> game.createTerrain(code);
				case ITEM -> game.createItem(code);
				case CREATURE -> throw new AssertionError("Effetti delle creature con ENG-23: " + code);
			};
			assertThat(effect.code()).isEqualTo(code);
		});
	}

	/** Spell di prova: l'avversario del proprietario perde 2 punti vita. */
	private static final class DrainSpell implements SpellEffect {

		static final String CODE = "TEST_DRAIN";

		@Override
		public String code() {
			return CODE;
		}

		@Override
		public void resolve(EffectContext ctx) {
			ctx.opponent().loseLifePoints(2);
			ctx.log().spellResolved(ctx.owner().side(), ctx.card(), "drains 2 life points", null);
		}
	}

	/** Hidden di prova con uno stato: conta le attivazioni. */
	private static final class CountingHidden implements HiddenEffect {

		static final String CODE = "TEST_COUNTER";

		private int triggers;

		@Override
		public String code() {
			return CODE;
		}

		@Override
		public void trigger(EffectContext ctx) {
			triggers++;
		}
	}

	/** Terreno di prova: +1 ATK e +2 DEF. */
	private static final class PlainTerrain implements TerrainEffect {

		static final String CODE = "TEST_PLAIN";

		@Override
		public String code() {
			return CODE;
		}

		@Override
		public int attackBonus() {
			return 1;
		}

		@Override
		public int defenseBonus() {
			return 2;
		}
	}

	/** Oggetto di prova, senza momenti finché non arriva ENG-33. */
	private static final class TestItem implements ItemEffect {

		static final String CODE = "TEST_ITEM";

		@Override
		public String code() {
			return CODE;
		}
	}
}
