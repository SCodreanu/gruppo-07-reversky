package it.downfall.reversky.engine.web;

/**
 * Richieste di esempio mostrate da Swagger UI per {@code POST /api/v1/simulations}: con "Try it out" danno subito
 * una partita vera invece dell'esempio generato da springdoc (id 0 e testi "string", che il motore rifiuta).
 * <p>
 * Sono gli esempi 01, 08 e 13 di examples/, con il risultato calcolato a mano in examples/README.md.
 * {@code OpenApiDocumentationTest} controlla che restino uguali a quei file.
 */
final class OpenApiExamples {

	/** Esempio 01: l'esempio del regolamento, una creatura per parte, finisce in un round (CHALLENGER_WIN). */
	static final String BASIC_MATCH = """
			{
			  "seed": 1,
			  "rules": {"rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75},
			  "factions": [
			    {"id": 1, "name": "Theory of the Wound"},
			    {"id": 2, "name": "Theory of the Invitation"},
			    {"id": 3, "name": "Theory of the Archive"},
			    {"id": 4, "name": "Theory of the Dream"}
			  ],
			  "factionBonuses": [
			    {"originFactionId": 2, "targetFactionId": 3, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 1, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 4, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 2, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 1, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 2, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 3, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 2, "targetFactionId": 4, "atkBonus": -1, "defBonus": 0}
			  ],
			  "challenger": {
			    "userId": 7,
			    "deck": [
			      {"cardId": 301, "name": "SCP-131", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 8, "def": 6, "subtype": null, "effectCode": null}
			    ]
			  },
			  "opponent": {
			    "userId": 12,
			    "deck": [
			      {"cardId": 305, "name": "Eternal Cup", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null}
			    ]
			  }
			}
			""";

	/** Esempio 08: partita completa da 10 carte con seed 42, bonus e malus di fazione (CHALLENGER_WIN, 14 round). */
	static final String FULL_MATCH = """
			{
			  "seed": 42,
			  "rules": {"rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75},
			  "factions": [
			    {"id": 1, "name": "Theory of the Wound"},
			    {"id": 2, "name": "Theory of the Invitation"},
			    {"id": 3, "name": "Theory of the Archive"},
			    {"id": 4, "name": "Theory of the Dream"}
			  ],
			  "factionBonuses": [
			    {"originFactionId": 2, "targetFactionId": 3, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 1, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 4, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 2, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 1, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 2, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 3, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 2, "targetFactionId": 4, "atkBonus": -1, "defBonus": 0}
			  ],
			  "challenger": {
			    "userId": 7,
			    "deck": [
			      {"cardId": 101, "name": "Dullers", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 3, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 102, "name": "Crawlers", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 102, "name": "Crawlers", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 103, "name": "Faceling", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 5, "def": 4, "subtype": null, "effectCode": null},
			      {"cardId": 105, "name": "Yellow Hum", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 6, "def": 3, "subtype": null, "effectCode": null},
			      {"cardId": 301, "name": "SCP-131", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 2, "def": 8, "subtype": null, "effectCode": null},
			      {"cardId": 305, "name": "Eternal Cup", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 111, "name": "Wretches", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 1, "secondaryFactionId": null, "atk": 6, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 311, "name": "SCP-049", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 3, "secondaryFactionId": null, "atk": 7, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 112, "name": "Faded Tag", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 1, "secondaryFactionId": null, "atk": 5, "def": 7, "subtype": null, "effectCode": null}
			    ]
			  },
			  "opponent": {
			    "userId": 12,
			    "deck": [
			      {"cardId": 201, "name": "Squonk", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 2, "secondaryFactionId": null, "atk": 3, "def": 7, "subtype": null, "effectCode": null},
			      {"cardId": 203, "name": "Ash Raven", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 2, "secondaryFactionId": null, "atk": 5, "def": 4, "subtype": null, "effectCode": null},
			      {"cardId": 203, "name": "Ash Raven", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 2, "secondaryFactionId": null, "atk": 5, "def": 4, "subtype": null, "effectCode": null},
			      {"cardId": 202, "name": "Will-o'-the-Wisp", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 2, "secondaryFactionId": null, "atk": 6, "def": 3, "subtype": null, "effectCode": null},
			      {"cardId": 404, "name": "Kodama", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 4, "secondaryFactionId": null, "atk": 4, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 401, "name": "Kasa-obake", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 4, "secondaryFactionId": null, "atk": 5, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 405, "name": "Temple Tanuki", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 4, "secondaryFactionId": null, "atk": 3, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 212, "name": "Wampus Cat", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 2, "secondaryFactionId": null, "atk": 7, "def": 5, "subtype": null, "effectCode": null},
			      {"cardId": 411, "name": "Noppera-bo", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 4, "secondaryFactionId": null, "atk": 6, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 213, "name": "White Stag", "type": "CREATURE", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": 2, "secondaryFactionId": null, "atk": 4, "def": 8, "subtype": null, "effectCode": null}
			    ]
			  }
			}
			""";

	/** Esempio 13: Spell, Hidden, Terreno e Oggetto in una partita breve (CHALLENGER_WIN, 3 round). */
	static final String SPECIAL_CARDS = """
			{
			  "seed": 2219,
			  "rules": {"rulesVersion": "1.0", "startingLifePoints": 30, "maxRounds": 75},
			  "factions": [
			    {"id": 1, "name": "Theory of the Wound"},
			    {"id": 2, "name": "Theory of the Invitation"},
			    {"id": 3, "name": "Theory of the Archive"},
			    {"id": 4, "name": "Theory of the Dream"}
			  ],
			  "factionBonuses": [
			    {"originFactionId": 2, "targetFactionId": 3, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 1, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 4, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 2, "atkBonus": 2, "defBonus": 0},
			    {"originFactionId": 4, "targetFactionId": 1, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 3, "targetFactionId": 2, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 1, "targetFactionId": 3, "atkBonus": -1, "defBonus": 0},
			    {"originFactionId": 2, "targetFactionId": 4, "atkBonus": -1, "defBonus": 0}
			  ],
			  "challenger": {
			    "userId": 7,
			    "deck": [
			      {"cardId": 96, "name": "Veil of the Anchored", "type": "SPELL", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": null, "secondaryFactionId": null, "atk": null, "def": null,
			        "subtype": "SHIELD", "effectCode": "VEIL_OF_ANCHORED"},
			      {"cardId": 115, "name": "Watching Rift", "type": "TERRAIN", "rarity": "Rare", "rarityOrder": 3,
			        "factionId": 3, "secondaryFactionId": null, "atk": null, "def": null,
			        "subtype": null, "effectCode": "WATCHING_RIFT"},
			      {"cardId": 301, "name": "SCP-131", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 8, "def": 6, "subtype": null, "effectCode": null},
			      {"cardId": 108, "name": "Echo Strike", "type": "HIDDEN", "rarity": "Rare", "rarityOrder": 3,
			        "factionId": null, "secondaryFactionId": null, "atk": null, "def": null,
			        "subtype": "DAMAGE", "effectCode": "ECHO_STRIKE"},
			      {"cardId": 304, "name": "Archive Puppet", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 3, "secondaryFactionId": null, "atk": 3, "def": 10, "subtype": null, "effectCode": null}
			    ]
			  },
			  "opponent": {
			    "userId": 12,
			    "deck": [
			      {"cardId": 117, "name": "Badge di Uscita Negata", "type": "ITEM", "rarity": "Rare", "rarityOrder": 3,
			        "factionId": 1, "secondaryFactionId": null, "atk": null, "def": null,
			        "subtype": null, "effectCode": "DENIED_EXIT_BADGE"},
			      {"cardId": 103, "name": "Faceling", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 7, "def": 9, "subtype": null, "effectCode": null},
			      {"cardId": 94, "name": "Skin Fracture", "type": "SPELL", "rarity": "Uncommon", "rarityOrder": 2,
			        "factionId": null, "secondaryFactionId": null, "atk": null, "def": null,
			        "subtype": "DAMAGE", "effectCode": "SKIN_FRACTURE"},
			      {"cardId": 102, "name": "Crawlers", "type": "CREATURE", "rarity": "Common", "rarityOrder": 1,
			        "factionId": 1, "secondaryFactionId": null, "atk": 4, "def": 5, "subtype": null, "effectCode": null}
			    ]
			  }
			}
			""";

	private OpenApiExamples() {
	}
}
