-- Le tre bustine del negozio dalla proposta DSG "Economia di gioco 2.0" (novembre 2026): Mini, Standard e Premium.
-- Tutti i prezzi e le probabilità sono [DA CONFERMARE]: la proposta li dichiara "da validare".
--
-- Proposta DSG (slot flessibile = Rare oppure, con le probabilità sotto, una rarità più alta):
--   Mini      300 crediti,  3 carte: 1 Common, 1 Uncommon, 1 slot Rare+       Mythic 7%,  Legendary 2,5%, Mutant 0,5%
--   Standard  600 crediti,  7 carte: 3 Common, 2 Uncommon, 1 Rare, 1 flessibile Mythic 10%, Legendary 5%,   Mutant 2,5%
--   Premium 1.200 crediti, 15 carte: 5 Common, 4 Uncommon, 3 Rare, 3 flessibili Mythic 22%, Legendary 10%, Mutant 3% per slot
--
-- Nel modello attuale (docs/pacchetti) lo slot flessibile è uno slot Rare che un bonus può sostituire, e ogni rarità
-- bonus si tira una volta per pacchetto:
-- - Mini e Standard coincidono con la proposta: lo slot Rare+ / flessibile è una Rare in più tra gli slot base.
-- - Premium ha 3 slot flessibili indipendenti, che il modello non esprime: si usa la probabilità di averne almeno uno
--   nei 3 slot (Mythic 1 − 0,78³ = 52,54%, Legendary 1 − 0,90³ = 27,10%, Mutant 1 − 0,97³ = 8,73%), con al massimo una
--   carta per rarità bonus. Gli slot flessibili veri sono un'evoluzione del modello da fare dopo il SAL.
--
-- Il Base Pack di V12 (stessa composizione della Standard, 100 crediti) diventa la Standard: chi lo ha già
-- nell'inventario o lo ha aperto trova la Standard, e le aperture già salvate restano valide.

UPDATE packs
SET name = 'Standard', cost = 600, image_path = 'packs/standard.webp'
WHERE name = 'Base Pack';

-- Mini: 1 Common, 1 Uncommon, 1 Rare + bonus Mythic 7%, Legendary 2,5%, Mutant 0,5%
WITH pack AS (
	INSERT INTO packs (name, cost, purchase_limit, image_path, is_active)
	VALUES ('Mini', 300, NULL, 'packs/mini.webp', TRUE)
	RETURNING id
), slots AS (
	INSERT INTO pack_rarity_rules (pack_id, rarity_id, quantity)
	SELECT pack.id, r.id, slot.quantity
	FROM pack
	CROSS JOIN (VALUES (1, 1), (2, 1), (3, 1)) AS slot (sorting_order, quantity)
	JOIN rarities r ON r.sorting_order = slot.sorting_order
	RETURNING pack_id
)
INSERT INTO pack_bonus_rules (pack_id, rarity_id, probability_units)
SELECT pack.id, r.id, bonus.units
FROM pack
CROSS JOIN (VALUES (4, 700), (5, 250), (6, 50)) AS bonus (sorting_order, units)
JOIN rarities r ON r.sorting_order = bonus.sorting_order;

-- Premium: 5 Common, 4 Uncommon, 6 Rare (3 Rare + 3 flessibili) + bonus Mythic 52,54%, Legendary 27,10%, Mutant 8,73%
WITH pack AS (
	INSERT INTO packs (name, cost, purchase_limit, image_path, is_active)
	VALUES ('Premium', 1200, NULL, 'packs/premium.webp', TRUE)
	RETURNING id
), slots AS (
	INSERT INTO pack_rarity_rules (pack_id, rarity_id, quantity)
	SELECT pack.id, r.id, slot.quantity
	FROM pack
	CROSS JOIN (VALUES (1, 5), (2, 4), (3, 6)) AS slot (sorting_order, quantity)
	JOIN rarities r ON r.sorting_order = slot.sorting_order
	RETURNING pack_id
)
INSERT INTO pack_bonus_rules (pack_id, rarity_id, probability_units)
SELECT pack.id, r.id, bonus.units
FROM pack
CROSS JOIN (VALUES (4, 5254), (5, 2710), (6, 873)) AS bonus (sorting_order, units)
JOIN rarities r ON r.sorting_order = bonus.sorting_order;
