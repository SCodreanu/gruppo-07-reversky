package it.downfall.reversky.api.pack.entity;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * Un'apertura di pacchetto (tabella {@code pack_openings}, docs/pacchetti): si scrive nella transazione
 * dell'apertura e non si modifica né si cancella. Serve al limite di acquisto e alle statistiche del backoffice.
 */
@Entity
@Table(name = "pack_openings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PackOpening {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Integer userId;

	@Column(name = "pack_id", nullable = false, updatable = false)
	private Integer packId;

	/** Id delle carte ricevute, nell'ordine degli slot. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "cards", nullable = false, updatable = false, columnDefinition = "jsonb")
	private List<Integer> cards;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public PackOpening(Integer userId, Integer packId, List<Integer> cards, Instant createdAt) {
		this.userId = userId;
		this.packId = packId;
		this.cards = List.copyOf(cards);
		this.createdAt = createdAt;
	}
}
