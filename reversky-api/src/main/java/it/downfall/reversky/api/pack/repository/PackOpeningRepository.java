package it.downfall.reversky.api.pack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.downfall.reversky.api.pack.entity.PackOpening;

public interface PackOpeningRepository extends JpaRepository<PackOpening, Integer> {

	/** Pacchetti di quel tipo già aperti dall'utente: contano nel limite di acquisto (docs/pacchetti). */
	public long countByUserIdAndPackId(Integer userId, Integer packId);

	/** Aperture dell'utente dalla più vecchia (usato dai test). */
	public List<PackOpening> findAllByUserIdOrderByIdAsc(Integer userId);
}
