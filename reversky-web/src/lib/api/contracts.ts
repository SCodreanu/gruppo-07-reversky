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
  battleId?: string | number;
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

/* Battles */

export type FriendSummary = { userId: number; username: string };
export type BattlePlayerSummary = { id: number; username: string };
export type BattleInvite = {
  id: string;
  challenger: BattlePlayerSummary;
  opponent: BattlePlayerSummary;
  deck: { id: number; name: string } | null;
  createdAt: string;
};
export type QueueTicket = {
  status: "WAITING";
  deckId: number;
  rating: number;
  queuedAt: string;
  window: number;
};
export type PlayerRanking = {
  rating: number;
  peakRating: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
};
export type BattleOutcome = "WIN" | "LOSS" | "DRAW";
export type BattleSummary = {
  battleId: string;
  createdAt: string;
  mode: "FRIENDLY" | "RANKED";
  opponent: BattlePlayerSummary;
  completed: boolean;
  outcome: BattleOutcome | null;
  endReason: string | null;
  myFinalLifePoints: number | null;
  opponentFinalLifePoints: number | null;
  rounds: number | null;
  ratingDelta: number | null;
};
export type BattleEvent = {
  sequence?: number;
  round?: number;
  type?: string;
  side?: "CHALLENGER" | "OPPONENT" | null;
  data?: Record<string, unknown>;
  message?: string;
};
export type BattleDetail = {
  id: string;
  mode: "FRIENDLY" | "RANKED";
  createdAt: string;
  completed: boolean;
  result: "CHALLENGER_WIN" | "OPPONENT_WIN" | "DRAW" | null;
  outcome: BattleOutcome | null;
  ratingBefore: number | null;
  ratingDelta: number | null;
  mySide: "CHALLENGER" | "OPPONENT";
  endReason: string | null;
  totalRounds: number | null;
  rulesVersion: string;
  seed: number;
  me: { id: number; username: string; finalLifePoints: number | null; creaturesDefeated: number | null; deck: unknown[] };
  opponent: { id: number; username: string; finalLifePoints: number | null; creaturesDefeated: number | null; deck: unknown[] };
  log: BattleEvent[];
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
