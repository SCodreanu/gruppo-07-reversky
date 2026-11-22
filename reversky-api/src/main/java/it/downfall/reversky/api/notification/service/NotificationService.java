package it.downfall.reversky.api.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.downfall.reversky.api.common.dto.PageResponse;
import it.downfall.reversky.api.notification.dto.NotificationResponse;
import it.downfall.reversky.api.notification.dto.UnreadCountResponse;
import it.downfall.reversky.api.notification.entity.Notification;
import it.downfall.reversky.api.notification.entity.NotificationType;
import it.downfall.reversky.api.notification.exception.NotificationNotFoundException;
import it.downfall.reversky.api.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Notifiche degli utenti (docs/notifiche, API-65): creazione per le altre funzionalità, lista, contatore delle non
 * lette e lettura. Ogni metodo pubblico per l'utente riceve l'id dal token e vede solo le sue notifiche.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final Clock clock;

	/**
	 * Crea una notifica per un utente, dentro la transazione del chiamante: se quella transazione viene annullata la
	 * notifica non resta. Lo usano amicizie, sfide e scambi (API-67, API-68, API-69).
	 * <p>
	 * Il {@code payload} contiene solo gli id delle entità coinvolte e id e username dell'altro utente, mai altri dati
	 * personali (la riga resta nel database finché non viene letta).
	 *
	 * @param userId  destinatario
	 * @param type    tipo della notifica
	 * @param payload id e username delle entità coinvolte
	 * @return la notifica salvata
	 * @throws org.springframework.transaction.IllegalTransactionStateException se non c'è una transazione aperta
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public NotificationResponse notify(Integer userId, NotificationType type, Map<String, Object> payload) {
		Notification notification = notificationRepository.save(
			new Notification(userId, type, payload, Instant.now(clock)));
		log.debug("Notification {} {} created for user {}", notification.getId(), type, userId);
		return NotificationResponse.from(notification);
	}

	/**
	 * Le mie notifiche, dalla più recente (GET /api/v1/notifications).
	 *
	 * @param unreadOnly true per avere solo quelle non lette
	 */
	@Transactional(readOnly = true)
	public PageResponse<NotificationResponse> findMine(Integer userId, boolean unreadOnly, Pageable pageable) {
		// L'ordine è fisso, dalla più recente: un sort chiesto dal client si ignora
		Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
		Page<Notification> notifications = unreadOnly
			? notificationRepository.findByUserIdAndReadAtIsNullOrderByIdDesc(userId, page)
			: notificationRepository.findByUserIdOrderByIdDesc(userId, page);
		return PageResponse.from(notifications.map(NotificationResponse::from));
	}

	/**
	 * Quante notifiche non lette ho (GET /api/v1/notifications/unread-count).
	 */
	@Transactional(readOnly = true)
	public UnreadCountResponse countUnread(Integer userId) {
		return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
	}

	/**
	 * Segna come letta una mia notifica (POST /api/v1/notifications/{id}/read). Una notifica già letta resta com'è.
	 *
	 * @throws NotificationNotFoundException se la notifica non esiste o è di un altro utente
	 */
	@Transactional
	public void markRead(Integer userId, Integer notificationId) {
		notificationRepository.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new NotificationNotFoundException(notificationId))
			.markRead(Instant.now(clock));
	}

	/**
	 * Segna come lette tutte le mie notifiche (POST /api/v1/notifications/read-all).
	 */
	@Transactional
	public void markAllRead(Integer userId) {
		int updated = notificationRepository.markAllRead(userId, Instant.now(clock));
		log.debug("User {} marked {} notifications as read", userId, updated);
	}

	/**
	 * Elimina le notifiche lette create prima di {@code before}, di tutti gli utenti: le usa la pulizia notturna.
	 *
	 * @return quante notifiche sono state eliminate
	 */
	@Transactional
	public int deleteReadCreatedBefore(Instant before) {
		return notificationRepository.deleteReadCreatedBefore(before);
	}
}
