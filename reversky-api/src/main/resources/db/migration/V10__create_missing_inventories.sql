-- Inventario per gli utenti registrati prima di API-17 (#87), deciso il 21 novembre.
-- Da qui la registrazione crea l'inventario nella stessa transazione dell'utente: questa migrazione serve solo ai
-- database di sviluppo che hanno già degli utenti, e su un database vuoto non inserisce niente.
-- Ogni utente senza inventario riceve i crediti di benvenuto del rule set attivo, con la sua riga SIGNUP.
WITH created AS (
	INSERT INTO inventories (user_id, credits)
	SELECT u.id, r.signup_bonus
	FROM users u
	CROSS JOIN rule_sets r
	WHERE r.is_active
		AND NOT EXISTS (SELECT 1 FROM inventories i WHERE i.user_id = u.id)
	RETURNING user_id, credits
)
INSERT INTO credit_transactions (user_id, amount, reason)
SELECT user_id, credits, 'SIGNUP'
FROM created;
