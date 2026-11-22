package it.downfall.reversky.api.trade.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.downfall.reversky.api.trade.entity.Trade;
import it.downfall.reversky.api.trade.entity.TradeStatus;
import jakarta.persistence.LockModeType;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

	/** Scambi aperti in cui tocca a me rispondere (GET /trades?box=incoming). */
	public default Page<Trade> findIncoming(Integer userId, Pageable pageable) {
		return findWaiting(userId, TradeStatus.WAITING_RECIPIENT, TradeStatus.WAITING_REQUESTER, pageable);
	}

	/** Scambi aperti in cui aspetto l'altro (GET /trades?box=outgoing). */
	public default Page<Trade> findOutgoing(Integer userId, Pageable pageable) {
		return findWaiting(userId, TradeStatus.WAITING_REQUESTER, TradeStatus.WAITING_RECIPIENT, pageable);
	}

	/**
	 * Scambi in cui l'utente è destinatario con {@code asRecipient} o richiedente con {@code asRequester}.
	 * <p>
	 * Gli stati sono parametri e non letterali JPQL: Hibernate scriverebbe il letterale come cast al tipo
	 * {@code tradestatus}, che non esiste (l'enum PostgreSQL è {@code trade_status}).
	 */
	@Query("""
		select t from Trade t
		where (t.recipientId = :userId and t.status = :asRecipient)
		   or (t.requesterId = :userId and t.status = :asRequester)
		""")
	public Page<Trade> findWaiting(@Param("userId") Integer userId, @Param("asRecipient") TradeStatus asRecipient,
			@Param("asRequester") TradeStatus asRequester, Pageable pageable);

	/**
	 * Legge lo scambio con {@code SELECT ... FOR UPDATE} (docs/scambi, "Accettazione"): accettazione, controproposta e
	 * rifiuto dello stesso scambio vanno uno alla volta, e chi arriva dopo vede lo stato già cambiato.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Trade t where t.id = :id")
	public Optional<Trade> findByIdForUpdate(@Param("id") UUID id);
}
