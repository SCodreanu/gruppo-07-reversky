package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import it.downfall.reversky.engine.core.model.CardDefinition;
import it.downfall.reversky.engine.core.model.CardType;
import it.downfall.reversky.engine.core.model.CreatureInPlay;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.MatchState;
import it.downfall.reversky.engine.core.model.PlayerInput;
import it.downfall.reversky.engine.core.model.PlayerResult;
import it.downfall.reversky.engine.core.model.PlayerState;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.model.Side;
import it.downfall.reversky.engine.core.rules.FactionMatrix;
import it.downfall.reversky.engine.core.rules.RuleConfig;
import it.downfall.reversky.engine.core.validation.SimulationInputValidator;

/**
 * Punto d'ingresso del motore: simula una partita completa e riproducibile (docs/architettura).
 * <p>
 * Non conserva stato tra una partita e l'altra: ogni chiamata crea il proprio {@link MatchState}, quindi la stessa
 * istanza può giocare partite con regole diverse, anche in parallelo. Stesso input e stesso seed danno la stessa
 * identica partita, log compreso (docs/mescolamento).
 */
public final class MatchSimulator {

	private final SimulationInputValidator validator = new SimulationInputValidator();

	/**
	 * Crea il simulatore.
	 */
	public MatchSimulator() {
	}

	/**
	 * Simula una partita.
	 * <p>
	 * Ordine: controllo dell'input, mescolamento (prima lo sfidante, poi l'avversario, con un solo generatore creato
	 * dal seed), pescata iniziale, poi round fino alla fine. Dopo ogni round chi non ha più una creatura in campo
	 * pesca (prima lo sfidante), poi si controlla la fine della partita; se la partita continua e c'è una nuova
	 * coppia di creature inizia un nuovo scontro.
	 *
	 * @param input regole, fazioni, bonus, mazzi e seed
	 * @return il risultato con il log round per round
	 * @throws it.downfall.reversky.engine.core.validation.InvalidSimulationInputException se l'input non è coerente
	 */
	public MatchResult simulate(SimulationInput input) {
		validator.validate(input);
		RuleConfig rules = input.rules();
		MatchState state = prepare(input);
		RoundResolver resolver = new RoundResolver(rules, new FactionMatrix(input.factionBonuses()));
		EndConditionChecker checker = new EndConditionChecker(rules);

		state.log().matchStarted(rules.startingLifePoints());
		drawCreature(state, state.challenger());
		drawCreature(state, state.opponent());
		resolver.startCombat(state);

		MatchEnd end = playUntilEnd(state, resolver, checker);
		state.log().matchEnded(end.result(), end.reason());
		return new MatchResult(end.result(), end.reason(), state.round(), result(state.challenger()),
				result(state.opponent()), input.seed(), rules.rulesVersion(), state.log().events());
	}

	/** Mescolamento lato server con il seed (docs/mescolamento): ordine fisso, prima lo sfidante. */
	private static MatchState prepare(SimulationInput input) {
		Random random = new Random(input.seed());
		List<CardDefinition> challengerDeck = shuffled(input.challenger(), random);
		List<CardDefinition> opponentDeck = shuffled(input.opponent(), random);
		int lifePoints = input.rules().startingLifePoints();
		return new MatchState(
				new PlayerState(Side.CHALLENGER, input.challenger().userId(), lifePoints, challengerDeck),
				new PlayerState(Side.OPPONENT, input.opponent().userId(), lifePoints, opponentDeck),
				random);
	}

	private static List<CardDefinition> shuffled(PlayerInput player, Random random) {
		List<CardDefinition> deck = new ArrayList<>(player.deck());
		Collections.shuffle(deck, random);
		return deck;
	}

	/**
	 * Round dopo round finché una condizione di fine è vera. Il limite di round (condizione 5) garantisce che il
	 * ciclo finisca anche con due creature che non si fanno danni.
	 */
	private static MatchEnd playUntilEnd(MatchState state, RoundResolver resolver, EndConditionChecker checker) {
		while (true) {
			resolver.resolve(state);
			boolean challengerDrew = refill(state, state.challenger());
			boolean opponentDrew = refill(state, state.opponent());
			Optional<MatchEnd> end = checker.check(state);
			if (end.isPresent()) {
				return end.get();
			}
			if (challengerDrew || opponentDrew) {
				resolver.startCombat(state);
			}
		}
	}

	/** Se il campo del giocatore è vuoto, pesca la creatura successiva. */
	private static boolean refill(MatchState state, PlayerState player) {
		return !player.hasCreature() && drawCreature(state, player);
	}

	/**
	 * Pesca finché esce una creatura e la mette in campo (regole-base, "Pescare"). Le altre carte pescate prima si
	 * registrano e si scartano; se nel mazzo restano solo carte non creatura si pescano tutte (D-06).
	 *
	 * @return {@code true} se una creatura è entrata in campo
	 */
	private static boolean drawCreature(MatchState state, PlayerState player) {
		Optional<CardDefinition> drawn = player.drawCard();
		while (drawn.isPresent()) {
			CardDefinition card = drawn.get();
			if (card.type() == CardType.CREATURE) {
				CreatureInPlay creature = new CreatureInPlay(card, player.side());
				player.placeCreature(creature);
				state.log().creatureEntered(creature);
				return true;
			}
			// TODO(@tommasofatticcioni): per ora Spell, Hidden, Terreni e Oggetti si scartano senza effetto;
			// vanno risolti con ENG-19 (#57), ENG-20 (#58), ENG-21 (#59) ed ENG-33 (#140)
			state.log().cardDrawn(player.side(), card);
			drawn = player.drawCard();
		}
		return false;
	}

	private static PlayerResult result(PlayerState player) {
		return new PlayerResult(player.userId(), player.lifePoints(), player.creaturesDefeated());
	}
}
