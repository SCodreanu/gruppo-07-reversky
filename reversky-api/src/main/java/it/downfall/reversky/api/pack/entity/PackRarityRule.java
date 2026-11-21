package it.downfall.reversky.api.pack.entity;

import it.downfall.reversky.api.catalog.entity.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Slot base di un pacchetto: quante carte di una rarità contiene (tabella {@code pack_rarity_rules}, docs/pacchetti).
 */
@Entity
@Table(name = "pack_rarity_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackRarityRule {

	@EmbeddedId
	private PackRuleId id;

	@MapsId("packId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pack_id", nullable = false)
	private Pack pack;

	@MapsId("rarityId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rarity_id", nullable = false)
	private Rarity rarity;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	PackRarityRule(Pack pack, Rarity rarity, int quantity) {
		this.id = new PackRuleId(pack.getId(), rarity.getId());
		this.pack = pack;
		this.rarity = rarity;
		this.quantity = quantity;
	}

	void changeQuantity(int quantity) {
		this.quantity = quantity;
	}

	public Integer getRarityId() {
		return id.getRarityId();
	}
}
