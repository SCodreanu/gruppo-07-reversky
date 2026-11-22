package it.downfall.reversky.api.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.catalog.entity.Effect;

public interface EffectRepository extends JpaRepository<Effect, String> {

	// Per tipo di carta (ordine dell'enum card_type di V1) e poi per codice
	List<Effect> findAllByOrderByAppliesToAscCodeAsc();
}
