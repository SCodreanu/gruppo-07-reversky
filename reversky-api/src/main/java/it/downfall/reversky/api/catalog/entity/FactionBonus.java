package it.downfall.reversky.api.catalog.entity;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
 * <p>
 * Implementa {@link Persistable}: la chiave è assegnata dal codice, quindi senza {@link #isNew()}
 * Spring Data considererebbe ogni nuova riga "già esistente" e per ognuna farebbe una SELECT prima
 * dell'INSERT ({@code merge} invece di {@code persist}). Con {@code isNew()} il PUT, che svuota la
 * tabella e reinserisce tutto, fa solo gli INSERT.
 */
@Entity
@Table(name = "faction_bonuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FactionBonus implements Persistable<FactionBonusId> {

	@EmbeddedId
	private FactionBonusId id;

	@Column(name = "atk_bonus", nullable = false)
	private Integer atkBonus;

	@Column(name = "def_bonus", nullable = false)
	private Integer defBonus;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	public FactionBonus(Integer originFactionId, Integer targetFactionId, Integer atkBonus, Integer defBonus) {
		this.id = new FactionBonusId(originFactionId, targetFactionId);
		this.atkBonus = atkBonus;
		this.defBonus = defBonus;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	/** Una riga letta dal database o appena salvata non è più nuova. */
	@PostLoad
	@PostPersist
	void markNotNew() {
		this.newEntity = false;
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
