package it.downfall.reversky.engine.core.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import it.downfall.reversky.engine.core.effect.EffectRegistry;
import it.downfall.reversky.engine.core.model.CardDefinition;
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

	private final SimulationInputValidator validator;
	private final DrawService drawService;

	/**
	 * Crea il simulatore.
	 *
	 * @param effects registro degli effetti: per il gioco {@link EffectRegistry#withAllEffects()}
	 */
	public MatchSimulator(EffectRegistry effects) {
		this.validator = new SimulationInputValidator(effects);
		this.drawService = new DrawService(effects);
	}

	/**
	 * Simula una partita.
	 * <p>
	 * Ordine: controllo dell'input, mescolamento (prima lo sfidante, poi l'avversario, con un solo generatore creato
	 * dal seed), poi un ciclo: chi non ha una creatura in campo pesca (prima lo sfidante), si controlla la fine della
	 * partita, se c'è una nuova coppia di creature inizia un nuovo scontro, e se entrambe le creature sono in campo si
	 * gioca un round. La pescata iniziale è il primo giro del ciclo.
	 *
	 * @param input regole, fazioni, bonus, mazzi e seed
	 * @return il risultato con il log round per round
	 * @throws it.downfall.reversky.engine.core.validation.InvalidSimulationInputException se l'input non è coerente
	 */
	public MatchResult simulate(SimulationInput input) {
		validator.validate(input);
		RuleConfig rules = input.rules();
		MatchState state = prepare(input);
		RoundResolver resolver = new RoundResolver(new FactionMatrix(input.factionBonuses()));
		EndConditionChecker checker = new EndConditionChecker(rules);

		state.log().matchStarted(rules.startingLifePoints());
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
	 * Pescata, controllo della fine, scontro e round, finché una condizione di fine è vera. Il limite di round
	 * (condizione 5) garantisce che il ciclo finisca anche con due creature che non si fanno danni.
	 * <p>
	 * Una creatura può morire anche fuori dal passo 4 (D-08): se all'inizio dello scontro ne resta una sola, il round
	 * non si gioca e il giro dopo pesca chi ha il campo vuoto. Un campo vuoto che non si può riempire è sempre una
	 * condizione di fine (3 o 4), quindi il ciclo non gira a vuoto.
	 */
	private MatchEnd playUntilEnd(MatchState state, RoundResolver resolver, EndConditionChecker checker) {
		while (true) {
			boolean challengerDrew = refill(state, state.challenger());
			boolean opponentDrew = refill(state, state.opponent());
			Optional<MatchEnd> end = checker.check(state);
			if (end.isPresent()) {
				return end.get();
			}
			if (!bothInPlay(state)) {
				// Una Spell dell'avversario ha ucciso la creatura appena pescata (D-08): si ripesca al giro dopo
				continue;
			}
			if (challengerDrew || opponentDrew) {
				resolver.startCombat(state);
			}
			if (bothInPlay(state)) {
				resolver.resolve(state);
			}
		}
	}

	/** Se il campo del giocatore è vuoto, pesca la creatura successiva. */
	private boolean refill(MatchState state, PlayerState player) {
		return !player.hasCreature() && drawService.drawCreature(state, player);
	}

	private static boolean bothInPlay(MatchState state) {
		return state.challenger().hasCreature() && state.opponent().hasCreature();
	}

	private static PlayerResult result(PlayerState player) {
		return new PlayerResult(player.userId(), player.lifePoints(), player.creaturesDefeated());
	}
}
