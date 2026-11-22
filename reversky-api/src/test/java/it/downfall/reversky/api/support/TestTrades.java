package it.downfall.reversky.api.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import it.downfall.reversky.api.trade.entity.Trade;
import it.downfall.reversky.api.trade.entity.TradeStatus;
import it.downfall.reversky.api.trade.repository.TradeRepository;
import it.downfall.reversky.api.user.entity.User;

/**
 * Prepara scambi di prova direttamente nel database (docs/test, "Dati di test"), senza passare dalla proposta: così i
 * test sulle letture e sugli stati non dipendono dai controlli sulle copie.
 * <p>
 * Come {@link TestCards} non è un {@code @TestComponent}: si crea con
 * {@code new TestTrades(tradeRepository, jdbcTemplate)}.
 */
public class TestTrades {

	private final TradeRepository tradeRepository;
	private final JdbcTemplate jdbcTemplate;

	public TestTrades(TradeRepository tradeRepository, JdbcTemplate jdbcTemplate) {
		this.tradeRepository = tradeRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Una proposta senza offerte da {@code requester} a {@code recipient}: tocca al destinatario.
	 */
	public UUID create(User requester, User recipient) {
		return tradeRepository.save(new Trade(requester.getId(), recipient.getId(), Instant.now())).getId();
	}

	/**
	 * Cambia lo stato senza passare dal service, es. per uno scambio già chiuso o in cui tocca al richiedente.
	 */
	public void setStatus(UUID tradeId, TradeStatus status) {
		jdbcTemplate.update("UPDATE trades SET status = ?::trade_status WHERE id = ?", status.name(), tradeId);
	}

	/**
	 * Cambia l'ultima mossa, per controllare l'ordine delle liste.
	 */
	public void setUpdatedAt(UUID tradeId, Instant updatedAt) {
		jdbcTemplate.update("UPDATE trades SET updated_at = ? WHERE id = ?", Timestamp.from(updatedAt), tradeId);
	}

	/**
	 * Lo stato salvato, letto dal database fuori dalla transazione del service.
	 */
	public TradeStatus statusOf(UUID tradeId) {
		return TradeStatus.valueOf(jdbcTemplate.queryForObject("SELECT status::text FROM trades WHERE id = ?",
			String.class, tradeId));
	}
}
