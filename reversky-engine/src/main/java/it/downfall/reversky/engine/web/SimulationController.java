package it.downfall.reversky.engine.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.downfall.reversky.engine.core.model.MatchResult;
import it.downfall.reversky.engine.core.simulation.MatchSimulator;
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
	 */
	@PostMapping
	public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
		MatchResult result = simulator.simulate(mapper.toCore(request));
		log.info("Simulazione completata: seed {}, {} round, {}", result.seed(), result.totalRounds(),
				result.result());
		return mapper.toResponse(result);
	}
}
