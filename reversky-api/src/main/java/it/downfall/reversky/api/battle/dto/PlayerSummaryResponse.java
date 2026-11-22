package it.downfall.reversky.api.battle.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un giocatore di una sfida: solo id e username.
 */
public record PlayerSummaryResponse(

	@Schema(example = "12")
	Integer id,

	@Schema(example = "sentinella_valle")
	String username
) {
}
