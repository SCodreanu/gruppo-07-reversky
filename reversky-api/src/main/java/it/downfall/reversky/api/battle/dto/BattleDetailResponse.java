package it.downfall.reversky.api.battle.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.battle.entity.BattleMode;
import it.downfall.reversky.api.battle.entity.BattleResult;
import it.downfall.reversky.api.battle.entity.DeckCardSnapshot;

/**
 * Dettaglio di una partita (GET /api/v1/battles/{id} e risposta dell'accettazione, docs/sfide), dal punto di vista di
 * chi la guarda: {@code me} sono io, {@code opponent} l'altro giocatore.
 *
 * @param mySide  {@code CHALLENGER} o {@code OPPONENT}: il campo {@code side} degli eventi del log usa questi valori
 * @param result  dal punto di vista della partita (null se non completata); {@code outcome} è il mio esito
 * @param ratingBefore il mio rating prima della partita, solo per le classificate completate (API-50)
 * @param ratingDelta  la variazione del mio rating, come {@code ratingDelta} dello storico; null per le amichevoli
 * @param log     eventi della partita così come li restituisce l'engine (reversky-engine/docs/log-partita)
 */
public record BattleDetailResponse(

	@Schema(example = "0b7e2f6a-3c1d-4f8e-9a2b-5d6c7e8f9a0b")
	UUID id,

	BattleMode mode,

	@Schema(example = "2026-11-21T10:20:00Z")
	Instant createdAt,

	@Schema(example = "true")
	boolean completed,

	@Schema(nullable = true)
	BattleResult result,

	@Schema(nullable = true)
	BattleOutcome outcome,

	@Schema(example = "1000", nullable = true,
		description = "My rating before this battle; null for friendly battles and while the battle is not completed")
	Integer ratingBefore,

	@Schema(example = "13", nullable = true,
		description = "Change of my rating; null for friendly battles and while the battle is not completed")
	Integer ratingDelta,

	@Schema(example = "OPPONENT")
	String mySide,

	@Schema(example = "LIFE_POINTS_ZERO", nullable = true)
	String endReason,

	@Schema(example = "37", nullable = true)
	Integer totalRounds,

	@Schema(example = "1.0")
	String rulesVersion,

	@Schema(example = "8812734650912")
	long seed,

	BattlePlayerResponse me,

	BattlePlayerResponse opponent,

	List<Object> log
) {

	/**
	 * Un giocatore della partita con il suo mazzo come era alla preparazione.
	 *
	 * @param finalLifePoints   null se la partita non è completata; può essere negativo
	 * @param creaturesDefeated creature avversarie sconfitte, null se la partita non è completata
	 */
	public record BattlePlayerResponse(

		@Schema(example = "12")
		Integer id,

		@Schema(example = "sentinella_valle")
		String username,

		@Schema(nullable = true)
		Integer finalLifePoints,

		@Schema(nullable = true)
		Integer creaturesDefeated,

		List<DeckCardSnapshot> deck
	) {
	}
}
