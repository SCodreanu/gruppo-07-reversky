package it.downfall.reversky.api.inventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import it.downfall.reversky.api.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

	/**
	 * Legge l'inventario con {@code SELECT ... FOR UPDATE}: le altre transazioni che vogliono bloccarlo aspettano
	 * la fine di questa. Evita le doppie spese con richieste contemporanee (docs/crediti, regola 4).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Inventory i where i.userId = :userId")
	public Optional<Inventory> findByUserIdForUpdate(Integer userId);
}
