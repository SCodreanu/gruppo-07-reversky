package it.downfall.reversky.api.catalog.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave composta di {@code faction_bonuses} (docs/database/schema.dbml).
 * <p>
 * {@code (origin_faction_id, target_faction_id)}: una sola riga per coppia di fazioni.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FactionBonusId implements Serializable {

	@Column(name = "origin_faction_id", nullable = false)
	private Integer originFactionId;

	@Column(name = "target_faction_id", nullable = false)
	private Integer targetFactionId;

	public FactionBonusId(Integer originFactionId, Integer targetFactionId) {
		this.originFactionId = originFactionId;
		this.targetFactionId = targetFactionId;
	}
}
