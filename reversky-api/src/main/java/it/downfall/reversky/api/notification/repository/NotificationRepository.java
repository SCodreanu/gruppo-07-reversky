package it.downfall.reversky.api.notification.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.notification.entity.Notification;

/**
 * Notifiche degli utenti ({@code notifications}). L'ordine è sempre per id decrescente, che segue l'ordine di
 * creazione e usa l'indice {@code idx_notifications_user_id_id}.
 */
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

	/** Le notifiche di un utente, dalla più recente. */
	Page<Notification> findByUserIdOrderByIdDesc(Integer userId, Pageable pageable);

	/** Le notifiche non lette di un utente, dalla più recente. */
	Page<Notification> findByUserIdAndReadAtIsNullOrderByIdDesc(Integer userId, Pageable pageable);

	long countByUserIdAndReadAtIsNull(Integer userId);

	/** Una notifica solo se è dell'utente: quella di un altro non si distingue da una che non esiste. */
	Optional<Notification> findByIdAndUserId(Integer id, Integer userId);

	/**
	 * Segna come lette tutte le notifiche non lette dell'utente.
	 *
	 * @return quante notifiche sono state segnate
	 */
	@Modifying
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int markAllRead(@Param("userId") Integer userId, @Param("now") Instant now);

	/**
	 * Elimina le notifiche già lette e create prima di {@code before}; quelle non lette restano sempre.
	 *
	 * @return quante notifiche sono state eliminate
	 */
	@Modifying
	@Query("delete from Notification n where n.readAt is not null and n.createdAt < :before")
	int deleteReadCreatedBefore(@Param("before") Instant before);
}
