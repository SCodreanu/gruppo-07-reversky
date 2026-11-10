-- Accettare aggiorna lo stato; rifiutare, ritirare o rimuovere elimina la riga.
-- La richiesta inversa a una già PENDING la accetta (gestita dal codice).
CREATE TABLE friendships (
	requester_id INTEGER NOT NULL REFERENCES users (id),
	recipient_id INTEGER NOT NULL REFERENCES users (id),
	status friendship_status NOT NULL DEFAULT 'PENDING',
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

	PRIMARY KEY (requester_id, recipient_id),
	CONSTRAINT chk_friendships_no_self_friendship CHECK (requester_id <> recipient_id)
);

-- Al massimo una riga per coppia di utenti, in qualunque direzione
CREATE UNIQUE INDEX uq_friendships_pair ON friendships (LEAST(requester_id, recipient_id), GREATEST(requester_id, recipient_id));
CREATE INDEX idx_friendships_recipient_id_status ON friendships (recipient_id, status);

-- Lo stato dice di chi è il turno. Gli scambi non scadono: restano aperti finché uno dei due non li chiude.
CREATE TABLE trades (
	id UUID PRIMARY KEY,
	requester_id INTEGER NOT NULL REFERENCES users (id),
	recipient_id INTEGER NOT NULL REFERENCES users (id),
	status trade_status NOT NULL DEFAULT 'WAITING_RECIPIENT',
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	-- Ultima offerta, controproposta o risposta
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

	CONSTRAINT chk_trades_no_self_trade CHECK (requester_id <> recipient_id)
);

CREATE INDEX idx_trades_requester_id ON trades (requester_id);
CREATE INDEX idx_trades_recipient_id_status ON trades (recipient_id, status);

-- user_id è il partecipante che cede la carta. Una controproposta aggiorna le righe una per una, mai svuotando e reinserendo.
-- Le copie di chi ha fatto l'offerta in corso restano riservate finché lo scambio è aperto.
CREATE TABLE trade_offers (
	trade_id UUID NOT NULL REFERENCES trades (id),
	user_id INTEGER NOT NULL REFERENCES users (id),
	card_id INTEGER NOT NULL REFERENCES catalog_cards (id),
	quantity INTEGER NOT NULL,

	PRIMARY KEY (trade_id, user_id, card_id),
	CONSTRAINT chk_trade_offers_quantity_positive CHECK (quantity > 0)
);

-- Calcolo delle copie riservate di un utente per una carta
CREATE INDEX idx_trade_offers_user_id_card_id ON trade_offers (user_id, card_id);
