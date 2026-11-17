package it.downfall.reversky.engine.web;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.model.SimulationInput;
import it.downfall.reversky.engine.core.simulation.MatchSimulator;
import it.downfall.reversky.engine.core.validation.InvalidSimulationInputException;
import it.downfall.reversky.engine.web.dto.SimulationRequest;
import it.downfall.reversky.engine.web.dto.SimulationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contratto REST dell'engine (docs/api-engine). L'unico cliente è {@code reversky-api}.
 * <p>
 * Fa solo DTO → core → {@link MatchSimulator} → core → DTO (CodingRequirements §4): le regole stanno nel core.
 */
@Slf4j
@Tag(name = "Simulations", description = "Match simulation")
@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
public class SimulationController {

	private final MatchSimulator simulator;
	private final SimulationMapper mapper;

	/**
	 * Simula una partita completa.
	 *
	 * @param request mazzi, regole, fazioni, bonus e seed
	 * @return il risultato con il log round per round
	 * @throws InvalidSimulationInputException se l'input non è coerente (400)
	 * @throws SimulationFailedException       se la simulazione fallisce per un bug (500, con il seed nel log)
	 */
	@Operation(summary = "Simulate a match", description = "Plays a full match with no player decisions and returns "
			+ "the result with the round-by-round log. The same request with the same seed always gives the same match. "
			+ "The checks run in a fixed order and the first one that fails is returned (see the code field).")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
			mediaType = MediaType.APPLICATION_JSON_VALUE,
			schema = @Schema(implementation = SimulationRequest.class),
			examples = {
					@ExampleObject(name = "basic", summary = "One creature each, ends in round 1",
							value = OpenApiExamples.BASIC_MATCH),
					@ExampleObject(name = "full", summary = "Full match with 10 cards each, seed 42",
							value = OpenApiExamples.FULL_MATCH) }))
	@ApiResponse(responseCode = "200", description = "Match simulated", content = @Content(
			mediaType = MediaType.APPLICATION_JSON_VALUE,
			schema = @Schema(implementation = SimulationResponse.class)))
	@ApiResponse(responseCode = "400", description = "Invalid input: malformed JSON, missing field, unknown faction, "
			+ "invalid faction bonus, invalid card factions, deck without creatures, invalid stats, missing subtype, "
			+ "missing or unknown effect. The code field says which check failed", content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = """
							{"title": "Invalid simulation input", "status": 400,
							 "detail": "Bonus of a faction against itself: 1 → 1", "code": "INVALID_FACTION_BONUS",
							 "instance": "/api/v1/simulations"}""")))
	@ApiResponse(responseCode = "500", description = "Bug in the engine: the seed is in the engine log",
			content = @Content(
					mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = """
							{"title": "Internal error", "status": 500,
							 "detail": "An unexpected error occurred in the game engine", "code": "INTERNAL_ERROR",
							 "instance": "/api/v1/simulations"}""")))
	@PostMapping
	public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
		SimulationInput input = mapper.toCore(request);
		MatchResult result;
		try {
			result = simulator.simulate(input);
		} catch (InvalidSimulationInputException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new SimulationFailedException(input.seed(), e);
		}
		log.info("Simulazione completata: seed {}, {} round, {}", result.seed(), result.totalRounds(),
				result.result());
		return mapper.toResponse(result);
	}
}
