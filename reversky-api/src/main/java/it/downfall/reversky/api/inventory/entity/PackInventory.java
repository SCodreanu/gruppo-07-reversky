package it.downfall.reversky.api.inventory.entity;

import org.springframework.data.domain.Persistable;

import it.downfall.reversky.api.pack.entity.Pack;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Pacchetti comprati e non ancora aperti di un tipo (tabella {@code pack_inventory}, docs/crediti e docs/pacchetti).
 * La quantità è sempre maggiore di 0: a 0 la riga si elimina.
 */
@Entity
@Table(name = "pack_inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackInventory implements Persistable<PackInventoryId> {

	@EmbeddedId
	private PackInventoryId id;

	@MapsId("packId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pack_id", nullable = false)
	private Pack pack;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	public PackInventory(Integer userId, Pack pack, int quantity) {
		this.id = new PackInventoryId(userId, pack.getId());
		this.pack = pack;
		this.quantity = quantity;
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

	public void add(int packs) {
		this.quantity += packs;
	}

	/** Toglie un pacchetto; il service elimina la riga quando arriva a 0. */
	public void removeOne() {
		this.quantity -= 1;
	}

	public Integer getPackId() {
		return id.getPackId();
	}
}
