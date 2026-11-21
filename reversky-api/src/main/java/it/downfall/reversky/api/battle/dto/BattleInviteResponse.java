package it.downfall.reversky.api.battle.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un invito di sfida in attesa (GET/POST /api/v1/battle-invites, docs/sfide).
 *
 * @param deck il mazzo dello sfidante, solo per lo sfidante: l'avversario lo scopre nella partita (deciso il
 *             21 novembre)
 */
public record BattleInviteResponse(

	@Schema(example = "0b7e2f6a-3c1d-4f8e-9a2b-5d6c7e8f9a0b")
	UUID id,

	PlayerSummaryResponse challenger,

	PlayerSummaryResponse opponent,

	@Schema(nullable = true)
	DeckRef deck,

	@Schema(example = "2026-11-21T10:00:00Z")
	Instant createdAt
) {

	/**
	 * Riferimento a un mazzo.
	 */
	public record DeckRef(

		@Schema(example = "3")
		Integer id,

		@Schema(example = "Alice Invitation")
		String name
	) {
	}
}
