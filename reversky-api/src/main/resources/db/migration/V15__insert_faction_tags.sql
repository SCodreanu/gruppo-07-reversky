-- Tag delle quattro fazioni (API-13, #83): un tag per fazione, usato dai filtri per tag di carte e pacchetti
-- (API-12, API-21). Si corregge dal backoffice (PUT /admin/factions/{id}/tags) o con una migrazione nuova, mai qui.
--
-- - Le fazioni si cercano per nome, quello inserito da V9 (D-44, nomi inglesi).
-- - Spell e Hidden non hanno fazione: questi tag non arrivano mai a loro (docs/pacchetti).
INSERT INTO tags (name) VALUES ('WOUND'), ('INVITATION'), ('ARCHIVE'), ('DREAM');

INSERT INTO faction_tags (faction_id, tag_id)
SELECT f.id, t.id
FROM factions f
JOIN (VALUES
	('Theory of the Wound', 'WOUND'),
	('Theory of the Invitation', 'INVITATION'),
	('Theory of the Archive', 'ARCHIVE'),
	('Theory of the Dream', 'DREAM')
) AS link (faction_name, tag_name) ON link.faction_name = f.name
JOIN tags t ON t.name = link.tag_name;
