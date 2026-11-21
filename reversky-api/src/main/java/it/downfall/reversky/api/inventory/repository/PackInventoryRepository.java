package it.downfall.reversky.api.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.inventory.entity.PackInventory;
import it.downfall.reversky.api.inventory.entity.PackInventoryId;

public interface PackInventoryRepository extends JpaRepository<PackInventory, PackInventoryId> {

	/** I pacchetti non aperti dell'utente con il loro tipo, per GET /inventory. */
	@EntityGraph(attributePaths = "pack")
	public List<PackInventory> findAllByIdUserIdOrderByIdPackIdAsc(Integer userId);

	public Optional<PackInventory> findByIdUserIdAndIdPackId(Integer userId, Integer packId);
}
