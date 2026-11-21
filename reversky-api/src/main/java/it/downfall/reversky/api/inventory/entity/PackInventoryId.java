package it.downfall.reversky.api.inventory.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chiave di {@code pack_inventory}: utente e tipo di pacchetto.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackInventoryId implements Serializable {

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "pack_id", nullable = false)
	private Integer packId;

	public PackInventoryId(Integer userId, Integer packId) {
		this.userId = userId;
		this.packId = packId;
	}
}
