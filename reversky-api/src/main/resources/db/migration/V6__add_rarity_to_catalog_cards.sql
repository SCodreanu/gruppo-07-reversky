-- NOT NULL senza un valore di DEFAULT visto che ogni carta avrà la propria rarità.
ALTER TABLE catalog_cards ADD COLUMN rarity_id INTEGER NOT NULL REFERENCES rarities (id);

CREATE INDEX idx_catalog_cards_rarity_id ON catalog_cards (rarity_id);
CREATE INDEX idx_catalog_cards_is_active_rarity_id ON catalog_cards (is_active, rarity_id);

