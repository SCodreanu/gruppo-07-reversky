-- Solo partite FRIENDLY: l'avversario deve essere un amico (controllato dal codice) e sceglie il mazzo quando accetta.
-- Gli inviti non scadono: accettare aggiorna lo stato; rifiutare o ritirare elimina la riga.
CREATE TABLE battle_invites (
	id UUID PRIMARY KEY,
	challenger_id INTEGER NOT NULL REFERENCES users (id),
	-- Diventa nullo quando il mazzo viene eliminato: gli inviti accettati non bloccano l'eliminazione
	challenger_deck_id INTEGER REFERENCES decks (id) ON DELETE SET NULL,
	opponent_id INTEGER NOT NULL REFERENCES users (id),
	status battle_invite_status NOT NULL DEFAULT 'PENDING',
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

	CONSTRAINT chk_battle_invites_no_self_challenge CHECK (challenger_id <> opponent_id),
	-- Un mazzo usato da un invito PENDING non si può eliminare (409, controllato dal codice)
	CONSTRAINT chk_battle_invites_pending_has_deck CHECK (status <> 'PENDING' OR challenger_deck_id IS NOT NULL)
);

CREATE INDEX idx_battle_invites_challenger_id ON battle_invites (challenger_id);
CREATE INDEX idx_battle_invites_challenger_deck_id ON battle_invites (challenger_deck_id);
CREATE INDEX idx_battle_invites_opponent_id_status ON battle_invites (opponent_id, status);

-- Al massimo un invito PENDING per direzione
CREATE UNIQUE INDEX uq_battle_invites_pending_pair ON battle_invites (challenger_id, opponent_id) WHERE status = 'PENDING';

-- FRIENDLY nasce da un invito, RANKED dal matchmaking (lo sfidante è chi è entrato in coda per primo).
-- result resta nullo finché la simulazione non è salvata; il salvataggio blocca la riga e si ferma se result è già valorizzato.
CREATE TABLE battles (
	id UUID PRIMARY KEY,
	mode battle_mode NOT NULL,
	-- Un invito produce al massimo una partita
	invite_id UUID REFERENCES battle_invites (id),
	challenger_id INTEGER NOT NULL REFERENCES users (id),
	opponent_id INTEGER NOT NULL REFERENCES users (id),
	result battle_result,
	-- Rule set attivo quando la partita è stata preparata; regole e bonus inviati all'engine sono copiati anche in battle_stats
	rule_set_id INTEGER NOT NULL REFERENCES rule_sets (id),

	-- Fotografia dell'Elo, solo RANKED, scritta nella stessa transazione di result
	challenger_rating_before INTEGER,
	challenger_rating_delta INTEGER,
	opponent_rating_before INTEGER,
	opponent_rating_delta INTEGER,

	-- Fotografie dei mazzi usati nella partita
	challenger_deck JSONB NOT NULL,
	opponent_deck JSONB NOT NULL,
	-- Seed, motivo di fine, punti vita finali...
	battle_stats JSONB NOT NULL,
	-- Eventi e risultati dei round
	rounds_log JSONB NOT NULL,

	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

	CONSTRAINT uq_battles_invite_id UNIQUE (invite_id),
	CONSTRAINT chk_battles_no_self_battle CHECK (challenger_id <> opponent_id),
	CONSTRAINT chk_battles_invite_matches_mode CHECK (
		(mode = 'FRIENDLY' AND invite_id IS NOT NULL)
		OR (mode = 'RANKED' AND invite_id IS NULL)
	),
	CONSTRAINT chk_battles_friendly_no_rating CHECK (
		mode = 'RANKED'
		OR (challenger_rating_before IS NULL AND challenger_rating_delta IS NULL
			AND opponent_rating_before IS NULL AND opponent_rating_delta IS NULL)
	),
	CONSTRAINT chk_battles_ranked_rating_complete CHECK (
		mode = 'FRIENDLY'
		OR result IS NULL
		OR (challenger_rating_before IS NOT NULL AND challenger_rating_delta IS NOT NULL
			AND opponent_rating_before IS NOT NULL AND opponent_rating_delta IS NOT NULL)
	)
);

-- Storico e anti-boosting
CREATE INDEX idx_battles_challenger_id_created_at ON battles (challenger_id, created_at);
CREATE INDEX idx_battles_opponent_id_created_at ON battles (opponent_id, created_at);
-- Una versione del rule set è già stata usata?
CREATE INDEX idx_battles_rule_set_id ON battles (rule_set_id);

-- Un ticket per utente. La riga si elimina quando l'utente esce dalla coda, viene abbinato o viene disattivato.
-- Le righe si leggono con FOR UPDATE SKIP LOCKED, così due esecuzioni non abbinano lo stesso utente.
CREATE TABLE matchmaking_queue (
	user_id INTEGER PRIMARY KEY REFERENCES users (id),
	-- Eliminare il mazzo elimina il ticket; modificarlo lo rimuove dal codice
	deck_id INTEGER NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
	-- Rating al momento dell'ingresso in coda
	rating INTEGER NOT NULL,
	queued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_matchmaking_queue_deck_id ON matchmaking_queue (deck_id);
CREATE INDEX idx_matchmaking_queue_rating ON matchmaking_queue (rating);
CREATE INDEX idx_matchmaking_queue_queued_at ON matchmaking_queue (queued_at);

-- Solo statistiche RANKED: la riga si crea al primo ingresso in coda.
-- Prima di aggiornare si bloccano le due righe con SELECT ... FOR UPDATE, user_id minore per primo.
CREATE TABLE player_ratings (
	user_id INTEGER PRIMARY KEY REFERENCES users (id),
	rating INTEGER NOT NULL DEFAULT 1000,
	peak_rating INTEGER NOT NULL DEFAULT 1000,
	games_played INTEGER NOT NULL DEFAULT 0,
	wins INTEGER NOT NULL DEFAULT 0,
	losses INTEGER NOT NULL DEFAULT 0,
	draws INTEGER NOT NULL DEFAULT 0,

	CONSTRAINT chk_player_ratings_rating_nonnegative CHECK (rating >= 0),
	CONSTRAINT chk_player_ratings_peak CHECK (peak_rating >= rating),
	CONSTRAINT chk_player_ratings_counters_nonnegative CHECK (wins >= 0 AND losses >= 0 AND draws >= 0),
	CONSTRAINT chk_player_ratings_games_total CHECK (games_played = wins + losses + draws)
);

-- Serve solo alla classifica [DA CONFERMARE]
CREATE INDEX idx_player_ratings_rating ON player_ratings (rating);
