-- Solo le creature hanno due fazioni (D-22, API-71, #146, deciso il 22 novembre): un Terreno o un Oggetto con
-- secondary_faction_id farebbe fallire ogni partita con 400 INVALID_CARD_FACTIONS dell'engine. Spell e Hidden non
-- hanno fazioni già per chk_catalog_cards_faction_matches_type. Nessuna carta di V11 ne ha una.
ALTER TABLE catalog_cards
	ADD CONSTRAINT chk_catalog_cards_secondary_faction_creature_only
		CHECK (type = 'CREATURE' OR secondary_faction_id IS NULL);
