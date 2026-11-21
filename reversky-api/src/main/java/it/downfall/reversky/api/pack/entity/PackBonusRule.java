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
 * Probabilità di una carta bonus di una rarità, in unità su 10.000 (tabella {@code pack_bonus_rules}, docs/pacchetti).
 */
@Entity
@Table(name = "pack_bonus_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackBonusRule {

	/** Le probabilità sono in unità su 10.000: 1.000 = 10%. */
	public static final int UNITS = 10_000;

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

	@Column(name = "probability_units", nullable = false)
	private Integer probabilityUnits;

	PackBonusRule(Pack pack, Rarity rarity, int probabilityUnits) {
		this.id = new PackRuleId(pack.getId(), rarity.getId());
		this.pack = pack;
		this.rarity = rarity;
		this.probabilityUnits = probabilityUnits;
	}

	void changeProbability(int probabilityUnits) {
		this.probabilityUnits = probabilityUnits;
	}

	public Integer getRarityId() {
		return id.getRarityId();
	}
}
