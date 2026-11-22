package it.downfall.reversky.api.notification.entity;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una notifica di un utente (tabella {@code notifications}, docs/notifiche, API-65). Si salva sempre, così il client
 * la ritrova anche se non era collegato quando è nata; l'id è anche l'id dell'evento SSE ({@code Last-Event-ID}).
 * <p>
 * Dopo la creazione cambia solo {@code read_at}: una notifica non si modifica e si cancella solo con la pulizia delle
 * notifiche lette e vecchie.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Integer userId;

	// Colonna di testo, non enum del database: niente NAMED_ENUM
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, updatable = false)
	private NotificationType type;

	/** Id e username delle entità coinvolte, mai altri dati personali. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> payload;

	/** Null finché l'utente non la legge. */
	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/**
	 * Notifica nuova, non letta.
	 *
	 * @param userId    destinatario
	 * @param type      tipo, anche nome dell'evento SSE
	 * @param payload   id e username delle entità coinvolte
	 * @param createdAt adesso, dal {@code Clock} dell'applicazione
	 */
	public Notification(Integer userId, NotificationType type, Map<String, Object> payload, Instant createdAt) {
		this.userId = userId;
		this.type = type;
		this.payload = Map.copyOf(payload);
		this.createdAt = createdAt;
	}

	public boolean isRead() {
		return readAt != null;
	}

	/**
	 * Segna la notifica come letta. Una notifica già letta tiene la sua ora di lettura.
	 */
	public void markRead(Instant now) {
		if (readAt == null) {
			readAt = now;
		}
	}
}
