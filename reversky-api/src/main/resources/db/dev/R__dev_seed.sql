-- Dati di prova SOLO per sviluppo (API-25, #95), deciso il 21 novembre. Si caricano con il profilo dev, che bootRun
-- attiva da solo (build.gradle); l'immagine Docker e i test non li vedono. Come attivarli: docs/database.
--
-- Migrazione ripetibile di Flyway: gira dopo quelle versionate e di nuovo solo quando questo file cambia. Ogni
-- istruzione inserisce soltanto ciò che manca, quindi si può rieseguire senza creare duplicati e senza toccare quello
-- che gli utenti di prova hanno cambiato nel frattempo.
--
-- Utenti (password di sviluppo per tutti: Reversky2026, mai usarla altrove):
-- - admin / admin@reversky.dev: ADMIN, senza carte;
-- - alice / alice@reversky.dev e bob / bob@reversky.dev: USER, amici tra loro, con tutte le carte originali attive
--   fino al limite di copie e un mazzo valido ciascuno (Alice Invitation, Bob Wound).

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

-- Crediti per provare il negozio (Mini 300, Standard 600, Premium 1.200 di V16): alice e bob arrivano a 10.000 una
-- volta sola, con una riga ADMIN_ADJUSTMENT come una correzione dal backoffice. Poi il saldo resta quello che spendono.
WITH before AS (
	SELECT i.user_id, i.credits
	FROM inventories i
	JOIN users u ON u.id = i.user_id
	WHERE u.email IN ('alice@reversky.dev', 'bob@reversky.dev')
		AND i.credits < 10000
		AND NOT EXISTS (SELECT 1 FROM credit_transactions t WHERE t.user_id = u.id AND t.reason = 'ADMIN_ADJUSTMENT')
), topped AS (
	UPDATE inventories i
	SET credits = 10000
	FROM before b
	WHERE i.user_id = b.user_id
	RETURNING i.user_id
)
INSERT INTO credit_transactions (user_id, amount, reason)
SELECT b.user_id, 10000 - b.credits, 'ADMIN_ADJUSTMENT'
FROM before b
JOIN topped t ON t.user_id = b.user_id;

-- Collezione completa: ogni carta originale attiva fino al suo limite di copie, min(deck_limit, copy_limit)
INSERT INTO collection_cards (user_id, card_id, quantity)
SELECT u.id, c.id, LEAST(r.deck_limit, COALESCE(c.copy_limit, r.deck_limit))
FROM users u
CROSS JOIN catalog_cards c
JOIN rarities r ON r.id = c.rarity_id
WHERE u.email IN ('alice@reversky.dev', 'bob@reversky.dev')
	AND c.is_active
	AND c.variant_of_id IS NULL
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

-- Un mazzo valido per il rule set 1.0 (30 carte): 7 creature Common da 2 copie e 6 Uncommon da 1 della fazione,
-- 3 Spell da 2 copie, una Hidden SURVIVAL e una DAMAGE, l'Oggetto e il Terreno della fazione. Le carte si scelgono
-- in ordine di id; le righe si inseriscono solo per un mazzo appena creato, mai in uno che esiste già.
WITH owner AS (
	SELECT id FROM users WHERE email = 'alice@reversky.dev'
), deck AS (
	INSERT INTO decks (user_id, name)
	SELECT owner.id, 'Alice Invitation' FROM owner
	WHERE NOT EXISTS (SELECT 1 FROM decks d WHERE d.user_id = owner.id AND d.name = 'Alice Invitation')
	RETURNING id
), faction AS (
	SELECT id FROM factions WHERE name = 'Theory of the Invitation'
), cards AS (
	(SELECT c.id, 2 AS quantity FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 1 AND c.faction_id = (SELECT id FROM faction)
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 7)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 2 AND c.faction_id = (SELECT id FROM faction)
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 6)
	UNION ALL
	(SELECT c.id, 2 FROM catalog_cards c WHERE c.type = 'SPELL' AND c.is_active AND c.variant_of_id IS NULL
		ORDER BY c.id LIMIT 3)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'SURVIVAL' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'DAMAGE' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type IN ('ITEM', 'TERRAIN') AND c.faction_id = (SELECT id FROM faction)
		AND c.is_active AND c.variant_of_id IS NULL)
)
INSERT INTO deck_cards (deck_id, card_id, quantity)
SELECT deck.id, cards.id, cards.quantity
FROM deck
CROSS JOIN cards;

-- Stesso mazzo per bob con la Theory of the Wound e le Spell successive
WITH owner AS (
	SELECT id FROM users WHERE email = 'bob@reversky.dev'
), deck AS (
	INSERT INTO decks (user_id, name)
	SELECT owner.id, 'Bob Wound' FROM owner
	WHERE NOT EXISTS (SELECT 1 FROM decks d WHERE d.user_id = owner.id AND d.name = 'Bob Wound')
	RETURNING id
), faction AS (
	SELECT id FROM factions WHERE name = 'Theory of the Wound'
), cards AS (
	(SELECT c.id, 2 AS quantity FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 1 AND c.faction_id = (SELECT id FROM faction)
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 7)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c JOIN rarities r ON r.id = c.rarity_id
		WHERE c.type = 'CREATURE' AND r.sorting_order = 2 AND c.faction_id = (SELECT id FROM faction)
			AND c.is_active AND c.variant_of_id IS NULL ORDER BY c.id LIMIT 6)
	UNION ALL
	(SELECT c.id, 2 FROM catalog_cards c WHERE c.type = 'SPELL' AND c.is_active AND c.variant_of_id IS NULL
		ORDER BY c.id OFFSET 3 LIMIT 3)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'SURVIVAL' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id OFFSET 1 LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type = 'HIDDEN' AND c.subtype = 'DAMAGE' AND c.is_active
		AND c.variant_of_id IS NULL ORDER BY c.id OFFSET 1 LIMIT 1)
	UNION ALL
	(SELECT c.id, 1 FROM catalog_cards c WHERE c.type IN ('ITEM', 'TERRAIN') AND c.faction_id = (SELECT id FROM faction)
		AND c.is_active AND c.variant_of_id IS NULL)
)
INSERT INTO deck_cards (deck_id, card_id, quantity)
SELECT deck.id, cards.id, cards.quantity
FROM deck
CROSS JOIN cards;
