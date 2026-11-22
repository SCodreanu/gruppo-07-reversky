package it.downfall.reversky.api.battle.entity;

import java.time.Duration;
import java.time.Instant;

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
 * Ticket di un utente nella coda classificata (tabella {@code matchmaking_queue}, docs/sfide, "Classificata:
 * matchmaking").
 * <p>
 * La chiave è l'id dell'utente: un solo ticket per utente. Il ticket sparisce quando l'utente esce dalla coda, viene
 * abbinato, modifica o elimina il mazzo, o il suo account viene disattivato.
 */
@Entity
@Table(name = "matchmaking_queue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchmakingTicket implements Persistable<Integer> {

	/** Finestra di ricerca appena entrati in coda: avversari entro ±100 punti di rating. */
	static final int INITIAL_WINDOW = 100;

	/** Ogni {@link #STEP_SECONDS} secondi di attesa la finestra si allarga di tanto. */
	static final int WINDOW_STEP = 50;

	static final int STEP_SECONDS = 10;

	/** La finestra non va oltre ±400 punti. */
	static final int MAX_WINDOW = 400;

	@Id
	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "deck_id", nullable = false)
	private Integer deckId;

	/** Rating al momento dell'ingresso in coda. */
	@Column(name = "rating", nullable = false)
	private int rating;

	@Column(name = "queued_at", nullable = false)
	private Instant queuedAt;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Ticket nuovo.
	 *
	 * @param userId   utente in coda
	 * @param deckId   mazzo scelto, già controllato
	 * @param rating   rating attuale dell'utente
	 * @param queuedAt momento dell'ingresso in coda
	 */
	public MatchmakingTicket(Integer userId, Integer deckId, int rating, Instant queuedAt) {
		this.userId = userId;
		this.deckId = deckId;
		this.rating = rating;
		this.queuedAt = queuedAt;
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

	/**
	 * Finestra di ricerca dell'avversario in punti di rating (±): {@code min(100 + 50 × (secondi di attesa / 10, per
	 * difetto), 400)}. Dopo 35 secondi vale 250.
	 *
	 * @param now adesso, dal {@code Clock} dell'applicazione
	 * @return la finestra, tra 100 e 400
	 */
	public int searchWindow(Instant now) {
		// Un orologio che va indietro non restringe la finestra sotto quella iniziale
		long waitedSeconds = Math.max(0, Duration.between(queuedAt, now).toSeconds());
		long window = INITIAL_WINDOW + WINDOW_STEP * (waitedSeconds / STEP_SECONDS);
		return (int) Math.min(window, MAX_WINDOW);
	}
}
