import type { CardSubtype, CardType, CatalogCard } from "@/lib/api/contracts";

export const CARD_TYPES: CardType[] = ["CREATURE", "SPELL", "HIDDEN", "ITEM", "TERRAIN"];

const typeLabels: Record<CardType, string> = {
  CREATURE: "Creature",
  SPELL: "Spell",
  HIDDEN: "Hidden",
  ITEM: "Item",
  TERRAIN: "Terrain",
};

const subtypeLabels: Record<CardSubtype, string> = {
  DAMAGE: "Damage",
  SHIELD: "Shield",
  BURN: "Burn",
  SURVIVAL: "Survival",
};

export function typeLabel(type: CardType): string {
  return typeLabels[type];
}

export function subtypeLabel(subtype: CardSubtype | null): string | null {
  return subtype ? subtypeLabels[subtype] : null;
}

/** "Theory of the Wound" -> "Wound": the cards show the short name. */
export function shortFactionName(name: string): string {
  return name.replace(/^Theory of the\s+/i, "");
}

export function factionLine(card: CatalogCard): string {
  if (!card.faction) return "Any Theory";
  const first = shortFactionName(card.faction.name);
  return card.secondaryFaction ? `${first} / ${shortFactionName(card.secondaryFaction.name)}` : first;
}

/** CSS tone key for a rarity name; unknown rarities fall back to "common". */
export function rarityTone(name: string): "common" | "uncommon" | "rare" | "mythic" | "legendary" | "mutant" {
  const key = name.toLowerCase();
  if (key === "uncommon" || key === "rare" || key === "mythic" || key === "legendary" || key === "mutant") return key;
  return "common";
}
