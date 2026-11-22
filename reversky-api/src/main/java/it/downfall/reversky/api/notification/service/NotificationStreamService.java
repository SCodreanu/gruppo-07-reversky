package it.downfall.reversky.api.notification.service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import it.downfall.reversky.api.auth.exception.AccountDisabledException;
import it.downfall.reversky.api.notification.dto.NotificationResponse;
import it.downfall.reversky.api.notification.entity.Notification;
import it.downfall.reversky.api.notification.repository.NotificationRepository;
import it.downfall.reversky.api.user.exception.UserNotFoundException;
import it.downfall.reversky.api.user.service.UserDisabledEvent;
import it.downfall.reversky.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Canale SSE delle notifiche (docs/notifiche, "Canale SSE", API-66): le connessioni aperte di ogni utente, l'invio
 * dopo il commit, il recupero con {@code Last-Event-ID}, il ping e la chiusura.
 * <p>
 * Le connessioni stanno in memoria, in questa istanza dell'API: con più istanze ognuna invierebbe solo alle proprie
 * connessioni (per la consegna gira un'istanza sola). Un utente può avere più connessioni, una per scheda aperta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStreamService {

	private final NotificationRepository notificationRepository;
	private final UserService userService;
	private final Clock clock;

	private final Map<Integer, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

	/**
	 * Apre una connessione dell'utente (GET /api/v1/notifications/stream). La connessione si chiude da sola quando
	 * scade l'access token che l'ha aperta: il client si ricollega con un token nuovo e {@code Last-Event-ID}.
	 * <p>
	 * La connessione si registra <b>prima</b> di rimandare le notifiche perse: una notifica nata in quel momento può
	 * arrivare due volte (il client riconosce l'id), ma non si perde mai.
	 *
	 * @param lastEventId    id dell'ultima notifica ricevuta; null alla prima connessione
	 * @param tokenExpiresAt scadenza dell'access token della richiesta
	 * @throws AccountDisabledException se l'account è stato disattivato, anche con un access token ancora valido
	 * @throws UserNotFoundException    se l'utente del token non esiste più
	 */
	public SseEmitter open(Integer userId, Integer lastEventId, Instant tokenExpiresAt) {
		if (!userService.getById(userId).isEnabled()) {
			throw new AccountDisabledException();
		}

		long timeout = Math.max(1, Duration.between(clock.instant(), tokenExpiresAt).toMillis());
		SseEmitter emitter = new SseEmitter(timeout);
		register(userId, emitter);
		if (lastEventId != null) {
			replay(userId, lastEventId, emitter);
		}
		log.debug("Notification stream opened for user {}: {} connections", userId, connections(userId));
		return emitter;
	}

	/**
	 * Dopo il commit della transazione che ha creato la notifica, la invia a tutte le connessioni del destinatario.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onNotificationCreated(NotificationCreatedEvent event) {
		Set<SseEmitter> open = emitters.get(event.userId());
		if (open == null) {
			return;
		}
		for (SseEmitter emitter : open) {
			send(event.userId(), emitter, toEvent(event.notification()));
		}
	}

	/**
	 * Un commento {@code : ping} su ogni connessione aperta, per tenerla viva dietro ai proxy; una connessione che non
	 * risponde più si toglie qui.
	 */
	public void sendHeartbeat() {
		emitters.forEach((userId, open) -> open.forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("ping"))));
	}

	/**
	 * Dopo il commit della disattivazione di un account chiude tutte le sue connessioni.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserDisabled(UserDisabledEvent event) {
		disconnect(event.userId());
	}

	/**
	 * Chiude tutte le connessioni dell'utente.
	 */
	public void disconnect(Integer userId) {
		Set<SseEmitter> open = emitters.remove(userId);
		if (open == null) {
			return;
		}
		open.forEach(SseEmitter::complete);
		log.info("Closed {} notification streams of user {}", open.size(), userId);
	}

	/**
	 * Quante connessioni ha aperto l'utente in questa istanza.
	 */
	public int connections(Integer userId) {
		Set<SseEmitter> open = emitters.get(userId);
		return open == null ? 0 : open.size();
	}

	private void register(Integer userId, SseEmitter emitter) {
		emitters.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(emitter);
		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> {
			remove(userId, emitter);
			emitter.complete();
		});
		emitter.onError(error -> remove(userId, emitter));
	}

	// Al massimo le 100 più recenti dopo lastEventId (le altre sono in GET /notifications), rimandate dalla più
	// vecchia così il client finisce sull'id più alto
	private void replay(Integer userId, Integer lastEventId, SseEmitter emitter) {
		List<Notification> missed = notificationRepository.findTop100ByUserIdAndIdGreaterThanOrderByIdDesc(userId,
			lastEventId);
		for (Notification notification : missed.reversed()) {
			send(userId, emitter, toEvent(NotificationResponse.from(notification)));
		}
	}

	private void send(Integer userId, SseEmitter emitter, SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException e) {
			// Il client se n'è andato o la connessione è già chiusa: si toglie, il client si ricollegherà
			remove(userId, emitter);
			log.debug("Notification stream of user {} removed: {}", userId, e.getMessage());
		}
	}

	private void remove(Integer userId, SseEmitter emitter) {
		emitters.computeIfPresent(userId, (key, open) -> {
			open.remove(emitter);
			return open.isEmpty() ? null : open;
		});
	}

	private static SseEventBuilder toEvent(NotificationResponse notification) {
		return SseEmitter.event()
			.id(String.valueOf(notification.id()))
			.name(notification.type().name())
			.data(notification, MediaType.APPLICATION_JSON);
	}
}
