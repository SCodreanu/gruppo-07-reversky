package it.downfall.reversky.api.factionbonus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Bonus/malus di una fazione di origine contro una fazione bersaglio (tabella
 * {@code faction_bonuses}, docs/backoffice "Dati di riferimento").
 * <p>
 * {@code atkBonus} può essere negativo (malus, es. -1). {@code defBonus} non può essere negativo.
 * Una fazione non ha bonus contro se stessa: vincolo {@code chk_faction_bonuses_no_self_matchup}
 * nel database, controllato anche dal service per restituire 422 e non 500.
 */
@Entity
@Table(name = "faction_bonuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FactionBonus {

	@EmbeddedId
	private FactionBonusId id;

	@Column(name = "atk_bonus", nullable = false)
	private Integer atkBonus;

	@Column(name = "def_bonus", nullable = false)
	private Integer defBonus;

	public FactionBonus(Integer originFactionId, Integer targetFactionId, Integer atkBonus, Integer defBonus) {
		this.id = new FactionBonusId(originFactionId, targetFactionId);
		this.atkBonus = atkBonus;
		this.defBonus = defBonus;
	}

	/** Comodità: l'id composto non serve chiamarlo ogni volta. */
	public Integer getOriginFactionId() {
		return id.getOriginFactionId();
	}

	/** Comodità: l'id composto non serve chiamarlo ogni volta. */
	public Integer getTargetFactionId() {
		return id.getTargetFactionId();
	}
}