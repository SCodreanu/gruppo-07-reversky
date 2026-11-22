/** Types that match the DTOs in reversky-api (see Swagger and reversky-api/docs). */
export type AuthTokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
};

export type UserProfile = {
  id: number;
  username: string;
  email: string;
  role: "USER" | "ADMIN";
  emailVerified: boolean;
};

export type FieldViolation = {
  field: string;
  message: string;
};

/** Spring ProblemDetail plus the API's `code`, validation errors and per-error extras. */
export type SpringProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldViolation[];
  /** DECK_INVALID: the rules the deck breaks. */
  violations?: DeckViolation[];
  /** ENGINE_UNAVAILABLE / ENGINE_INPUT_REJECTED: the battle to retry. */
  battleId?: number;
};

export type ApiResult<T = unknown> = {
  ok: boolean;
  status: number;
  body: T | SpringProblemDetail | null;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/* Catalog */

export type CardType = "CREATURE" | "SPELL" | "HIDDEN" | "ITEM" | "TERRAIN";
export type CardSubtype = "DAMAGE" | "SHIELD" | "BURN" | "SURVIVAL";

export type RaritySummary = { id: number; name: string };
export type FactionSummary = { id: number; name: string };
export type Tag = { id: number; name: string };

export type Rarity = RaritySummary & {
  deckLimit: number;
  /** Null means no limit. */
  maxPerDeck: number | null;
  sortingOrder: number;
};

export type Faction = FactionSummary & {
  emblemPath: string | null;
  description: string | null;
};

export type CatalogCard = {
  id: number;
  name: string;
  type: CardType;
  rarity: RaritySummary;
  /** Null for SPELL and HIDDEN cards. */
  faction: FactionSummary | null;
  secondaryFaction: FactionSummary | null;
  subtype: CardSubtype | null;
  effectCode: string | null;
  variantOfId: number | null;
  atk: number | null;
  def: number | null;
  copyLimit: number | null;
  imagePath: string | null;
  /** Public path on the API server; placeholders return 404 until the art is uploaded. */
  imageUrl: string | null;
  effectDescription: string | null;
  flavorText: string | null;
  active: boolean;
  /** Only in GET /cards/{id}. */
  tags?: Tag[];
};

/* Shop and inventory */

export type PackSlot = { rarity: RaritySummary; quantity: number };
export type PackBonus = { rarity: RaritySummary; probabilityUnits: number; percentage: number };

export type Pack = {
  id: number;
  name: string;
  cost: number;
  purchaseLimit: number | null;
  /** Only with a token and a purchase limit. */
  remainingPurchases: number | null;
  imagePath: string | null;
  cardCount: number;
  slots: PackSlot[];
  bonuses: PackBonus[];
};

export type InventoryPack = { packId: number; name: string; imagePath: string | null; quantity: number };
export type Inventory = { credits: number; packs: InventoryPack[] };

export type PackPurchaseRequest = { quantity: number };

export type OpenPackResponse = {
  packId: number;
  remainingPacks: number;
  cards: { card: CatalogCard; isNew: boolean }[];
};

/* Collection */

export type CollectionEntry = {
  card: CatalogCard;
  quantity: number;
  reservedQuantity: number;
  usedInDecks: number;
};

export type RarityProgress = { rarity: RaritySummary; owned: number; total: number };
export type CollectionSummary = { owned: number; total: number; rarities: RarityProgress[] };

/* Decks */

export type DeckViolation = { code: string; message: string; cardId: number | null };
export type DeckCardRequest = { cardId: number; quantity: number };
export type DeckRequest = { name: string; cards: DeckCardRequest[] };
export type DeckValidationRequest = { cards: DeckCardRequest[] };
export type DeckValidationResponse = { valid: boolean; violations: DeckViolation[] };
export type DeckSummary = { id: number; name: string; cardCount: number; valid: boolean };
export type Deck = DeckSummary & {
  cards: { card: CatalogCard; quantity: number }[];
  violations: DeckViolation[];
};

export type DeckRules = {
  deckSize: number;
  deckCreatures: number;
  deckSpells: number;
  deckHidden: number;
  deckItems: number;
  deckTerrains: number;
  maxHiddenSurvival: number;
  maxHiddenDamage: number;
  rarities: Rarity[];
};
