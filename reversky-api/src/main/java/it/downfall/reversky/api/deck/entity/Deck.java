package it.downfall.reversky.api.deck.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un mazzo di un utente (tabella {@code decks}, docs/mazzi). Il nome è univoco per utente.
 */
@Entity
@Table(name = "decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deck {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Integer userId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	public Deck(Integer userId, String name) {
		this.userId = userId;
		this.name = name;
	}

	public void rename(String name) {
		this.name = name;
	}
}
