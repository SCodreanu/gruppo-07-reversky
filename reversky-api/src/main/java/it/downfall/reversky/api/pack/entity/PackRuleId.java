package it.downfall.reversky.api.pack.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave di {@code pack_rarity_rules} e {@code pack_bonus_rules}: pacchetto e rarità.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackRuleId implements Serializable {

	@Column(name = "pack_id", nullable = false)
	private Integer packId;

	@Column(name = "rarity_id", nullable = false)
	private Integer rarityId;

	public PackRuleId(Integer packId, Integer rarityId) {
		this.packId = packId;
		this.rarityId = rarityId;
	}
}
