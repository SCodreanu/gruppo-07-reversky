package it.downfall.reversky.api.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.inventory.entity.CreditTransaction;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Integer> {

	/** Movimenti di un utente dal più vecchio, per ricostruire il saldo (usato dai test). */
	public List<CreditTransaction> findAllByUserIdOrderByIdAsc(Integer userId);
}
