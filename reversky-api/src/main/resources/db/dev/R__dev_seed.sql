-- Dati di prova SOLO per sviluppo (API-25, #95), deciso il 21 novembre. Si caricano con il profilo dev, che bootRun
-- attiva da solo (build.gradle); l'immagine Docker e i test non li vedono. Come attivarli: docs/database.
--
-- Migrazione ripetibile di Flyway: gira dopo quelle versionate e di nuovo solo quando questo file cambia. Ogni
-- istruzione inserisce soltanto ciò che manca, quindi si può rieseguire senza creare duplicati e senza toccare quello
-- che gli utenti di prova hanno cambiato nel frattempo.
--
-- Utenti (password di sviluppo per tutti: Reversky2026, mai usarla altrove):
-- - ogni account nel database dev: tutte le carte attive fino al limite di copie e tre mazzi validi
--   (Invitation, Wound, Archive); admin resta ADMIN, alice e bob sono amici.

-- Utenti con email verificata e privacy e termini accettati
INSERT INTO users (role_id, username, email, password_hash, email_verified_at, privacy_accepted_at, terms_accepted_at)
SELECT r.id, seed.username, seed.email, '$2a$10$8F62oRzP8qrYULaLrL9uxeEmzpVBWcwF4h4CUhL3tiCGeEwzhGdx2', now(), now(), now()
FROM (VALUES ('admin', 'admin@reversky.dev', 'ADMIN'),
             ('alice', 'alice@reversky.dev', 'USER'),
             ('bob', 'bob@reversky.dev', 'USER')) AS seed (username, email, role_name)
JOIN roles r ON r.name = seed.role_name
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = seed.email OR u.username = seed.username);

-- Inventario con i crediti di benvenuto del rule set attivo e la riga SIGNUP, come la registrazione
WITH created AS (
	INSERT INTO inventories (user_id, credits)
	SELECT u.id, rs.signup_bonus
	FROM users u
	CROSS JOIN rule_sets rs
	WHERE rs.is_active
		AND u.email IN ('admin@reversky.dev', 'alice@reversky.dev', 'bob@reversky.dev')
		AND NOT EXISTS (SELECT 1 FROM inventories i WHERE i.user_id = u.id)
	RETURNING user_id, credits
)
INSERT INTO credit_transactions (user_id, amount, reason)
SELECT user_id, credits, 'SIGNUP'
FROM created;

-- Collezione completa per tutti gli account nel database dev: ogni carta attiva fino al suo limite di copie,
-- min(deck_limit, copy_limit). Le copie già presenti restano invariate.
INSERT INTO collection_cards (user_id, card_id, quantity)
SELECT u.id, c.id, LEAST(r.deck_limit, COALESCE(c.copy_limit, r.deck_limit))
FROM users u
JOIN catalog_cards c ON c.is_active
JOIN rarities r ON r.id = c.rarity_id
ON CONFLICT (user_id, card_id) DO NOTHING;

-- alice e bob sono amici, così possono sfidarsi in amichevole
INSERT INTO friendships (requester_id, recipient_id, status)
SELECT alice.id, bob.id, 'ACCEPTED'
FROM users alice
JOIN users bob ON bob.email = 'bob@reversky.dev'
WHERE alice.email = 'alice@reversky.dev'
	AND NOT EXISTS (
		SELECT 1 FROM friendships f
		WHERE LEAST(f.requester_id, f.recipient_id) = LEAST(alice.id, bob.id)
			AND GREATEST(f.requester_id, f.recipient_id) = GREATEST(alice.id, bob.id)
	);

-- Tre mazzi validi per ciascun account sul rule set iniziale (30 carte): 7 creature Common da 2 copie,
-- 6 Uncommon da 1, 3 Spell da 2, una Hidden per sottotipo, un Oggetto e un Terreno della fazione.
-- Le carte si scelgono in ordine di id; si aggiungono contenuti solo ai mazzi creati qui, mai a quelli esistenti.
WITH templates (suffix, faction_name, spell_offset, hidden_offset) AS (
	VALUES ('Invitation', 'Theory of the Invitation', 0, 0),
	       ('Wound', 'Theory of the Wound', 3, 1),
	       ('Archive', 'Theory of the Archive', 6, 2)
), owners AS (
	SELECT id, username FROM users
), specs AS (
	SELECT owners.id AS user_id,
	       initcap(owners.username) || ' ' || templates.suffix AS deck_name,
	       templates.faction_name, templates.spell_offset, templates.hidden_offset
	FROM owners CROSS JOIN templates
), created AS (
	INSERT INTO decks (user_id, name)
	SELECT specs.user_id, specs.deck_name
	FROM specs
	WHERE NOT EXISTS (SELECT 1 FROM decks d WHERE d.user_id = specs.user_id AND d.name = specs.deck_name)
	RETURNING id, user_id, name
)
INSERT INTO deck_cards (deck_id, card_id, quantity)
SELECT created.id, cards.id, cards.quantity
FROM created
JOIN specs ON specs.user_id = created.user_id AND specs.deck_name = created.name
JOIN factions faction ON faction.name = specs.faction_name
CROSS JOIN LATERAL (
	(SELECT c.id, 2 AS quantity FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 1 AND c.faction_id = faction.id
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 7)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 2 AND c.faction_id = faction.id
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 6)
	UNION ALL
	(SELECT c.id, 2 FROM catalog_cards c WHERE c.type = 'SPELL' AND c.is_active AND c.variant_of_id IS NULL
		ORDER BY c.id OFFSET specs.spell_offset LIMIT 3)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'SURVIVAL' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id OFFSET specs.hidden_offset LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'DAMAGE' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id OFFSET specs.hidden_offset LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type IN ('ITEM', 'TERRAIN') AND c.faction_id = faction.id
		AND c.is_active AND c.variant_of_id IS NULL)
) cards;
