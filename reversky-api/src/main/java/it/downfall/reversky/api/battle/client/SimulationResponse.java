package it.downfall.reversky.api.battle.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Risposta di {@code POST /api/v1/simulations} dell'engine (reversky-engine/docs/api-engine).
 *
 * @param result     {@code CHALLENGER_WIN}, {@code OPPONENT_WIN} o {@code DRAW}, come l'enum {@code battle_result}
 * @param log        eventi della partita, salvati così come arrivano in {@code battles.rounds_log}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulationResponse(
	String result,
	String endReason,
	int totalRounds,
	PlayerResult challenger,
	PlayerResult opponent,
	long seed,
	String rulesVersion,
	List<Object> log
) {

	/**
	 * @param finalLifePoints può essere negativo (D-43)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PlayerResult(Integer userId, int finalLifePoints, int creaturesDefeated) {
	}
}
