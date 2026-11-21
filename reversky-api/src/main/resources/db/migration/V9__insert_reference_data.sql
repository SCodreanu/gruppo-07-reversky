-- Dati di riferimento del gioco (API-15, #85), deciso il 20 novembre.
-- Si correggono dal backoffice o con una migrazione nuova, mai qui (regola 8 di AGENTS.md).

-- Le sei rarità del catalogo. Special (D-23, sorting_order 7) arriva con la prima carta speciale.
-- [DA CONFERMARE] deck_limit (Q-02) e max_per_deck (Q-03): proposte del bilanciamento (API-64);
-- Terreno e Oggetto contano tra le Rare nel massimo di 5.
INSERT INTO rarities (name, deck_limit, max_per_deck, sorting_order) VALUES
	('Common', 3, NULL, 1),
	('Uncommon', 2, NULL, 2),
	('Rare', 2, 5, 3),
	('Mythic', 1, 2, 4),
	('Legendary', 1, 2, 5),
	('Mutant', 1, 1, 6);

-- Le quattro fazioni nell'ordine degli esempi dell'engine: su un database vuoto ricevono gli id 1-4
-- (Wound 1, Invitation 2, Archive 3, Dream 4). Senza id espliciti, così la sequenza dell'identity resta
-- allineata e il primo POST /admin/factions riceve il 5. Emblemi segnaposto finché non ci sono le
-- illustrazioni; descrizione vuota finché i DSG non danno il testo.
INSERT INTO factions (name, emblem_path) VALUES ('Theory of the Wound', 'factions/wound.svg');
INSERT INTO factions (name, emblem_path) VALUES ('Theory of the Invitation', 'factions/invitation.svg');
INSERT INTO factions (name, emblem_path) VALUES ('Theory of the Archive', 'factions/archive.svg');
INSERT INTO factions (name, emblem_path) VALUES ('Theory of the Dream', 'factions/dream.svg');

-- Nessuna riga in faction_bonuses: il bonus di fazione (D-04) aspetta la decisione dei DSG dopo il report di
-- bilanciamento (ENG-28) e si inserisce dal backoffice (PUT /admin/faction-bonuses).

-- Rule set 1.0 attivo, con il mazzo da 30 della scheda di progettazione (20 creature, 6 Spell, 2 Hidden,
-- 1 Oggetto, 1 Terreno) e i massimi delle Hidden per sottotipo (Q-24).
-- [DA CONFERMARE] punti vita (D-01), crediti di benvenuto (Q-11), ricompense delle classificate (Q-12).
-- Il limite di 75 round totali è deciso (D-02). Nessun autore: è il seed, non un admin.
INSERT INTO rule_sets (
	version, is_active,
	starting_life_points, max_rounds,
	deck_size, deck_creatures, deck_spells, deck_hidden, deck_items, deck_terrains,
	max_hidden_survival, max_hidden_damage,
	signup_bonus, win_reward, draw_reward
) VALUES (
	'1.0', TRUE,
	30, 75,
	30, 20, 6, 2, 1, 1,
	2, 1,
	500, 30, 10
);
