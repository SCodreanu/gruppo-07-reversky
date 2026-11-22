package it.downfall.reversky.api.battle.entity;

import org.springframework.data.domain.Persistable;

import it.downfall.reversky.api.battle.dto.BattleOutcome;
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
 * Rating Elo e statistiche classificate di un utente (tabella {@code player_ratings}, docs/sfide).
 * <p>
 * La chiave è l'id dell'utente. La riga nasce al primo ingresso nella coda classificata con il rating iniziale; prima
 * l'utente non ha una riga e {@code GET /rankings/me} mostra i valori iniziali. Le amichevoli non la toccano mai.
 */
@Entity
@Table(name = "player_ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRating implements Persistable<Integer> {

	/** Rating di chi non ha ancora giocato una classificata (lo stesso default della colonna). */
	public static final int INITIAL_RATING = 1000;

	@Id
	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "rating", nullable = false)
	private int rating;

	@Column(name = "peak_rating", nullable = false)
	private int peakRating;

	@Column(name = "games_played", nullable = false)
	private int gamesPlayed;

	@Column(name = "wins", nullable = false)
	private int wins;

	@Column(name = "losses", nullable = false)
	private int losses;

	@Column(name = "draws", nullable = false)
	private int draws;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Rating nuovo: {@link #INITIAL_RATING}, nessuna partita giocata.
	 *
	 * @param userId utente a cui appartiene
	 */
	public PlayerRating(Integer userId) {
		this.userId = userId;
		this.rating = INITIAL_RATING;
		this.peakRating = INITIAL_RATING;
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
	 * Applica una partita classificata finita: somma la variazione al rating, aggiorna il massimo e conta la partita
	 * tra vittorie, sconfitte o pareggi. Così restano sempre veri i controlli della tabella ({@code peak_rating >=
	 * rating}, {@code games_played = wins + losses + draws}).
	 *
	 * @param outcome esito della partita per questo giocatore
	 * @param delta   variazione calcolata con {@code Elo.eloDelta} sul rating e le partite di prima
	 * @throws IllegalArgumentException se il rating scenderebbe sotto 0: {@code eloDelta} non lo permette mai
	 */
	public void recordGame(BattleOutcome outcome, int delta) {
		if (rating + delta < 0) {
			throw new IllegalArgumentException("Rating below 0: " + rating + " + (" + delta + ")");
		}
		rating += delta;
		peakRating = Math.max(peakRating, rating);
		gamesPlayed++;
		switch (outcome) {
			case WIN -> wins++;
			case LOSS -> losses++;
			case DRAW -> draws++;
		}
	}
}
