package it.downfall.reversky.api.inventory.entity;

import java.time.Instant;

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
 * Un movimento del saldo (tabella {@code credit_transactions}, docs/crediti).
 * <p>
 * Le righe si scrivono nella stessa transazione della modifica del saldo e non si modificano né si cancellano:
 * per questo l'entità non ha setter.
 */
@Entity
@Table(name = "credit_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Integer userId;

	/** Positivo se guadagnati, negativo se spesi, 0 per un pacchetto gratuito. */
	@Column(name = "amount", nullable = false, updatable = false)
	private Integer amount;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "reason", nullable = false, updatable = false)
	private CreditReason reason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public CreditTransaction(Integer userId, int amount, CreditReason reason, Instant createdAt) {
		this.userId = userId;
		this.amount = amount;
		this.reason = reason;
		this.createdAt = createdAt;
	}
}
