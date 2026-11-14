package it.downfall.reversky.api.ruleset.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import it.downfall.reversky.api.ruleset.entity.RuleSet;

/**
 * Riga sintetica per GET /admin/rule-sets.
 */
public record RuleSetSummaryResponse(

	@Schema(example = "1")
	Integer id,

	@Schema(example = "1.0")
	String version,

	@Schema(example = "true")
	Boolean isActive,

	Instant createdAt
) {

	public static RuleSetSummaryResponse from(RuleSet ruleSet) {
		return new RuleSetSummaryResponse(
			ruleSet.getId(),
			ruleSet.getVersion(),
			ruleSet.getIsActive(),
			ruleSet.getCreatedAt());
	}
}