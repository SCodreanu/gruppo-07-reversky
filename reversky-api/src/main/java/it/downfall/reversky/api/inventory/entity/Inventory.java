package it.downfall.reversky.api.inventory.entity;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Inventario di un utente: il saldo dei crediti (tabella {@code inventories}, docs/crediti).
 * <p>
 * La chiave è l'id dell'utente. Il saldo cambia solo tramite {@code InventoryService}, che blocca la riga
 * prima di modificarla e scrive ogni movimento in {@code credit_transactions}.
 */
@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory implements Persistable<Integer> {

	@Id
	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "credits", nullable = false)
	private Integer credits;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Inventario nuovo, con il saldo iniziale.
	 */
	public Inventory(Integer userId, int credits) {
		this.userId = userId;
		this.credits = credits;
	}

	@Override
	public Integer getId() {
		return userId;
	}

	// La chiave è assegnata da noi: senza questo save() farebbe una SELECT prima dell'INSERT
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

	/** Aggiunge crediti; il service controlla che l'importo non sia negativo. */
	public void addCredits(int amount) {
		this.credits += amount;
	}

	/** Toglie crediti; il service controlla prima che il saldo basti. */
	public void removeCredits(int amount) {
		this.credits -= amount;
	}
}
