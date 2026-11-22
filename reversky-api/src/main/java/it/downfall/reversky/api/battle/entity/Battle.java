package it.downfall.reversky.api.battle.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una partita (tabella {@code battles}, docs/sfide). Si crea alla preparazione senza risultato; il risultato arriva
 * dall'engine nella seconda transazione, oppure con il retry.
 * <p>
 * Le fotografie dei mazzi, le statistiche e il log sono colonne {@code jsonb}. Il log può essere grande: le liste dello
 * storico non caricano l'entità ma solo i campi che servono.
 */
@Entity
@Table(name = "battles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Battle implements Persistable<UUID> {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "mode", nullable = false, updatable = false)
	private BattleMode mode;

	@Column(name = "invite_id", updatable = false)
	private UUID inviteId;

	@Column(name = "challenger_id", nullable = false, updatable = false)
	private Integer challengerId;

	@Column(name = "opponent_id", nullable = false, updatable = false)
	private Integer opponentId;

	/** Null finché l'engine non ha simulato la partita. */
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "result")
	private BattleResult result;

	@Column(name = "rule_set_id", nullable = false, updatable = false)
	private Integer ruleSetId;

	// Solo RANKED, scritte con il risultato (recordRatings): per le amichevoli restano null (chk_battles_friendly_no_rating)
	@Column(name = "challenger_rating_before")
	private Integer challengerRatingBefore;

	@Column(name = "challenger_rating_delta")
	private Integer challengerRatingDelta;

	@Column(name = "opponent_rating_before")
	private Integer opponentRatingBefore;

	@Column(name = "opponent_rating_delta")
	private Integer opponentRatingDelta;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "challenger_deck", nullable = false, updatable = false, columnDefinition = "jsonb")
	private List<DeckCardSnapshot> challengerDeck;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "opponent_deck", nullable = false, updatable = false, columnDefinition = "jsonb")
	private List<DeckCardSnapshot> opponentDeck;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "battle_stats", nullable = false, columnDefinition = "jsonb")
	private BattleStats battleStats;

	/** Eventi della partita così come li restituisce l'engine (reversky-engine/docs/log-partita). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rounds_log", nullable = false, columnDefinition = "jsonb")
	private List<Object> roundsLog;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** {@code true} solo per una riga creata con il costruttore e non ancora salvata. */
	@Transient
	@Getter(AccessLevel.NONE)
	private boolean newEntity = true;

	/**
	 * Partita amichevole preparata dall'accettazione di un invito, ancora senza risultato.
	 */
	public static Battle friendly(UUID inviteId, Integer challengerId, Integer opponentId, Integer ruleSetId,
			List<DeckCardSnapshot> challengerDeck, List<DeckCardSnapshot> opponentDeck, BattleStats stats, Instant createdAt) {
		return prepared(BattleMode.FRIENDLY, inviteId, challengerId, opponentId, ruleSetId, challengerDeck, opponentDeck,
			stats, createdAt);
	}

	/**
	 * Partita classificata preparata dall'abbinamento di due ticket, senza invito e ancora senza risultato
	 * ({@code chk_battles_invite_matches_mode}). Lo sfidante è chi era in coda da più tempo.
	 */
	public static Battle ranked(Integer challengerId, Integer opponentId, Integer ruleSetId,
			List<DeckCardSnapshot> challengerDeck, List<DeckCardSnapshot> opponentDeck, BattleStats stats, Instant createdAt) {
		return prepared(BattleMode.RANKED, null, challengerId, opponentId, ruleSetId, challengerDeck, opponentDeck,
			stats, createdAt);
	}

	private static Battle prepared(BattleMode mode, UUID inviteId, Integer challengerId, Integer opponentId,
			Integer ruleSetId, List<DeckCardSnapshot> challengerDeck, List<DeckCardSnapshot> opponentDeck, BattleStats stats,
			Instant createdAt) {
		Battle battle = new Battle();
		battle.id = UUID.randomUUID();
		battle.mode = mode;
		battle.inviteId = inviteId;
		battle.challengerId = challengerId;
		battle.opponentId = opponentId;
		battle.ruleSetId = ruleSetId;
		battle.challengerDeck = List.copyOf(challengerDeck);
		battle.opponentDeck = List.copyOf(opponentDeck);
		battle.battleStats = stats;
		battle.roundsLog = List.of();
		battle.createdAt = createdAt;
		return battle;
	}

	@Override
	public boolean isNew() {
		return newEntity;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.newEntity = false;
	}

	public boolean isCompleted() {
		return result != null;
	}

	public boolean involves(Integer userId) {
		return challengerId.equals(userId) || opponentId.equals(userId);
	}

	/**
	 * Salva l'esito dell'engine. Il service controlla prima che la partita non sia già completata.
	 */
	public void complete(BattleResult result, BattleStats stats, List<Object> roundsLog) {
		this.result = result;
		this.battleStats = stats;
		this.roundsLog = List.copyOf(roundsLog);
	}

	/**
	 * Fotografia dell'Elo di una classificata (API-50): i rating dei due giocatori prima della partita e le loro
	 * variazioni. Si scrive nella stessa transazione di {@link #complete}, così la riga rispetta
	 * {@code chk_battles_ranked_rating_complete}.
	 *
	 * @throws IllegalStateException per un'amichevole, che non ha mai rating ({@code chk_battles_friendly_no_rating})
	 */
	public void recordRatings(int challengerBefore, int challengerDelta, int opponentBefore, int opponentDelta) {
		if (mode != BattleMode.RANKED) {
			throw new IllegalStateException("Only ranked battles have ratings: battle " + id);
		}
		this.challengerRatingBefore = challengerBefore;
		this.challengerRatingDelta = challengerDelta;
		this.opponentRatingBefore = opponentBefore;
		this.opponentRatingDelta = opponentDelta;
	}
}
