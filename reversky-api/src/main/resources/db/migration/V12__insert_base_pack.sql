-- Pacchetto base del negozio (API-63, #133), deciso il 21 novembre. Si corregge dal backoffice o con una migrazione
-- nuova, mai qui (regola 8 di AGENTS.md).
--
-- - [DA CONFERMARE] costo 100 crediti (Q-13): con i 500 crediti di benvenuto se ne comprano 5. Nessun limite.
-- - Slot base 3 Common, 2 Uncommon, 2 Rare (7 carte) e bonus Mythic 10%, Legendary 5%, Mutant 2,5%, come l'esempio di
--   docs/pacchetti. Nessun pack_tags: escono tutte le carte attive e originali.
-- - Attivo subito: il catalogo di V11 ha carte di tutte queste rarità. Rarità per sorting_order, mai per nome.
-- - [DA CONFERMARE] image_path segnaposto finché non c'è l'illustrazione.
WITH pack AS (
	INSERT INTO packs (name, cost, purchase_limit, image_path, is_active)
	VALUES ('Base Pack', 100, NULL, 'packs/base.webp', TRUE)
	RETURNING id
), slots AS (
	INSERT INTO pack_rarity_rules (pack_id, rarity_id, quantity)
	SELECT pack.id, r.id, slot.quantity
	FROM pack
	CROSS JOIN (VALUES (1, 3), (2, 2), (3, 2)) AS slot (sorting_order, quantity)
	JOIN rarities r ON r.sorting_order = slot.sorting_order
	RETURNING pack_id
)
INSERT INTO pack_bonus_rules (pack_id, rarity_id, probability_units)
SELECT pack.id, r.id, bonus.units
FROM pack
CROSS JOIN (VALUES (4, 1000), (5, 500), (6, 250)) AS bonus (sorting_order, units)
JOIN rarities r ON r.sorting_order = bonus.sorting_order;
