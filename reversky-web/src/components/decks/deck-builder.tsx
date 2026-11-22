"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { CardDetailDialog } from "@/components/cards/card-detail-dialog";
import { CardFilters, EMPTY_FILTERS, Pager, useReferenceData, type CardFilterValues } from "@/components/cards/card-filters";
import { CARD_TYPES, rarityTone, typeLabel } from "@/components/cards/card-meta";
import { CardTile } from "@/components/cards/card-tile";
import { usePaged } from "@/components/cards/use-paged";
import { gameFetch, problemMessage, queryString } from "@/lib/api/browser";
import type {
  CardType,
  CatalogCard,
  CollectionEntry,
  Deck,
  DeckCardRequest,
  DeckRequest,
  DeckRules,
  DeckValidationResponse,
  DeckViolation,
  Rarity,
} from "@/lib/api/contracts";
import cardStyles from "@/components/cards/cards.module.css";
import styles from "./decks.module.css";

const PAGE_SIZE = 18;

type DeckEntry = { card: CatalogCard; quantity: number };
type Loaded = { rules: DeckRules; deck: Deck | null };

function requiredByType(rules: DeckRules): Record<CardType, number> {
  return {
    CREATURE: rules.deckCreatures,
    SPELL: rules.deckSpells,
    HIDDEN: rules.deckHidden,
    ITEM: rules.deckItems,
    TERRAIN: rules.deckTerrains,
  };
}

/** Copies allowed by the rarity and the card's own limit; the server also checks variants and balance limits. */
function copyLimit(card: CatalogCard, rarities: Rarity[]): number {
  const rarityLimit = rarities.find((rarity) => rarity.id === card.rarity.id)?.deckLimit ?? Number.POSITIVE_INFINITY;
  return Math.min(rarityLimit, card.copyLimit ?? Number.POSITIVE_INFINITY);
}

export function DeckBuilder({ deckId }: { deckId: number | null }) {
  const router = useRouter();
  const loginNext = deckId === null ? "/decks/new" : `/decks/${deckId}`;
  const { rarities, factions } = useReferenceData();
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [loadError, setLoadError] = useState("");
  const [name, setName] = useState("");
  const [entries, setEntries] = useState<Record<number, DeckEntry>>({});
  const [validation, setValidation] = useState<{ key: string; result: DeckValidationResponse } | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [saveViolations, setSaveViolations] = useState<DeckViolation[]>([]);
  const [savedMessage, setSavedMessage] = useState("");
  const [filters, setFilters] = useState<CardFilterValues>(EMPTY_FILTERS);
  const [pageIndex, setPageIndex] = useState(0);
  const [selected, setSelected] = useState<CatalogCard | null>(null);

  useEffect(() => {
    let active = true;
    void Promise.all([
      gameFetch<DeckRules>("decks/rules"),
      deckId === null ? Promise.resolve(null) : gameFetch<Deck>(`decks/${deckId}`),
    ]).then(([rulesResult, deckResult]) => {
      if (!active) return;
      if ((!rulesResult.ok && rulesResult.status === 401) || (deckResult && !deckResult.ok && deckResult.status === 401)) {
        router.replace(`/login?next=${encodeURIComponent(loginNext)}`);
        return;
      }
      if (!rulesResult.ok) {
        setLoadError(problemMessage(rulesResult.problem));
        return;
      }
      if (deckResult && !deckResult.ok) {
        setLoadError(deckResult.status === 404 ? "This deck doesn’t exist or isn’t yours." : problemMessage(deckResult.problem));
        return;
      }
      const deck = deckResult?.data ?? null;
      setLoaded({ rules: rulesResult.data, deck });
      if (deck) {
        setName(deck.name);
        setEntries(Object.fromEntries(deck.cards.map((row) => [row.card.id, { card: row.card, quantity: row.quantity }])));
      }
    });
    return () => {
      active = false;
    };
  }, [deckId, loginNext, router]);

  const deckCards: DeckCardRequest[] = useMemo(
    () => Object.values(entries).map((entry) => ({ cardId: entry.card.id, quantity: entry.quantity })),
    [entries],
  );
  const validationKey = JSON.stringify(deckCards);

  // Live validation against the server rules (ownership, copies, balance), debounced while editing.
  useEffect(() => {
    if (!loaded) return;
    let active = true;
    const timer = window.setTimeout(() => {
      void gameFetch<DeckValidationResponse>("decks/validate", { method: "POST", body: { cards: deckCards } }).then((result) => {
        if (active && result.ok) setValidation({ key: validationKey, result: result.data });
      });
    }, 350);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [loaded, deckCards, validationKey]);

  const query = queryString({
    name: filters.name.trim(),
    type: filters.type,
    rarityId: filters.rarityId,
    factionId: filters.factionId,
    sort: "type,asc",
    page: pageIndex,
    size: PAGE_SIZE,
  });
  const collection = usePaged<CollectionEntry>("collection", query, loginNext);

  function changeQuantity(card: CatalogCard, delta: number) {
    setSavedMessage("");
    setSaveViolations([]);
    setEntries((current) => {
      const quantity = (current[card.id]?.quantity ?? 0) + delta;
      const next = { ...current };
      if (quantity <= 0) delete next[card.id];
      else next[card.id] = { card, quantity };
      return next;
    });
  }

  async function save() {
    setSaving(true);
    setSaveError("");
    setSaveViolations([]);
    setSavedMessage("");
    const body: DeckRequest = { name: name.trim(), cards: deckCards };
    const result = deckId === null
      ? await gameFetch<Deck>("decks", { method: "POST", body })
      : await gameFetch<Deck>(`decks/${deckId}`, { method: "PUT", body });
    setSaving(false);
    if (!result.ok) {
      setSaveError(result.problem.errors?.[0]?.message ?? problemMessage(result.problem));
      setSaveViolations(result.problem.violations ?? []);
      return;
    }
    setSavedMessage("Deck saved.");
    if (deckId === null) router.replace(`/decks/${result.data.id}`);
  }

  if (loadError) {
    return (
      <div className={cardStyles.state} role="alert">
        <p>{loadError}</p>
        <Link className="button button--quiet" href="/decks">Back to your decks</Link>
      </div>
    );
  }
  if (!loaded) return <div className={cardStyles.state} aria-live="polite"><p>Opening the deck builder…</p></div>;

  const { rules } = loaded;
  const required = requiredByType(rules);
  const list = Object.values(entries).sort((a, b) =>
    CARD_TYPES.indexOf(a.card.type) - CARD_TYPES.indexOf(b.card.type) || a.card.name.localeCompare(b.card.name));
  const countByType = Object.fromEntries(CARD_TYPES.map((type) => [
    type,
    list.filter((entry) => entry.card.type === type).reduce((total, entry) => total + entry.quantity, 0),
  ])) as Record<CardType, number>;
  const total = list.reduce((sum, entry) => sum + entry.quantity, 0);
  const currentValidation = validation?.key === validationKey ? validation.result : null;
  const violations = saveViolations.length ? saveViolations : currentValidation?.violations ?? [];
  const canSave = name.trim().length > 0 && currentValidation?.valid === true && !saving;

  return (
    <div className={styles.builder}>
      <section className={styles.picker} aria-labelledby="picker-title">
        <h2 id="picker-title" className="display-type">Your collection</h2>
        <CardFilters
          values={filters}
          onChange={(values) => {
            setFilters(values);
            setPageIndex(0);
          }}
          rarities={rarities}
          factions={factions}
          resultLabel={collection.page ? `${collection.page.totalElements} cards${collection.loading ? " · updating…" : ""}` : "Loading…"}
        />
        {collection.error && <p className="input-error" role="alert">{collection.error}</p>}
        {collection.page && (
          <div className={cardStyles.grid} aria-busy={collection.loading}>
            {collection.page.content.map((entry) => {
              const inDeck = entries[entry.card.id]?.quantity ?? 0;
              const available = entry.quantity - entry.reservedQuantity;
              const max = Math.min(available, copyLimit(entry.card, rarities));
              return (
                <CardTile
                  key={entry.card.id}
                  card={entry.card}
                  badge={inDeck ? `${inDeck}/${entry.quantity}` : `×${entry.quantity}`}
                  dimmed={!entry.card.active}
                  onSelect={setSelected}
                  footer={(
                    <span className={styles.stepper}>
                      <button type="button" aria-label={`Remove one ${entry.card.name}`} disabled={inDeck === 0} onClick={() => changeQuantity(entry.card, -1)}>−</button>
                      <span>{inDeck}</span>
                      <button type="button" aria-label={`Add one ${entry.card.name}`} disabled={!entry.card.active || inDeck >= max} onClick={() => changeQuantity(entry.card, 1)}>+</button>
                    </span>
                  )}
                />
              );
            })}
          </div>
        )}
        {collection.page && <Pager page={collection.page.page} totalPages={collection.page.totalPages} onPage={setPageIndex} />}
      </section>

      <aside className={styles.panel} aria-labelledby="deck-title">
        <p className="eyebrow">{deckId === null ? "New deck" : "Edit deck"}</p>
        <h2 id="deck-title" className="display-type">{name.trim() || "Untitled deck"}</h2>
        <label className="form-field">
          <span className="field-label">Deck name</span>
          <input className="text-input" maxLength={50} value={name} onChange={(event) => {
            setName(event.target.value);
            setSavedMessage("");
          }} placeholder="Name your deck" />
        </label>

        <ul className={styles.counters}>
          <li className={total === rules.deckSize ? styles.counterOk : undefined}>
            <span>Total</span><strong>{total}/{rules.deckSize}</strong>
          </li>
          {CARD_TYPES.map((type) => (
            <li key={type} className={countByType[type] === required[type] ? styles.counterOk : undefined}>
              <span>{typeLabel(type)}</span><strong>{countByType[type]}/{required[type]}</strong>
            </li>
          ))}
        </ul>

        <div className={styles.status} aria-live="polite">
          {currentValidation === null ? (
            <span className={styles.muted}>Checking the rules…</span>
          ) : currentValidation.valid ? (
            <span className={styles.valid}>The deck follows every rule.</span>
          ) : (
            <span className={styles.invalid}>{violations.length} {violations.length === 1 ? "rule" : "rules"} to fix</span>
          )}
        </div>
        {violations.length > 0 && (
          <ul className={styles.violations}>
            {violations.map((violation, index) => <li key={`${violation.code}-${violation.cardId ?? index}`}>{violation.message}</li>)}
          </ul>
        )}

        {list.length > 0 && (
          <ul className={styles.deckCards}>
            {list.map((entry) => (
              <li key={entry.card.id} className={cardStyles[`tone-${rarityTone(entry.card.rarity.name)}`]}>
                <span className={styles.deckCardName}>
                  <span className={styles.toneDot} aria-hidden="true" />
                  {entry.card.name}
                  <small>{typeLabel(entry.card.type)}</small>
                </span>
                <span className={styles.stepper}>
                  <button type="button" aria-label={`Remove one ${entry.card.name}`} onClick={() => changeQuantity(entry.card, -1)}>−</button>
                  <span>{entry.quantity}</span>
                  <button type="button" aria-label={`Add one ${entry.card.name}`} disabled={entry.quantity >= copyLimit(entry.card, rarities)} onClick={() => changeQuantity(entry.card, 1)}>+</button>
                </span>
              </li>
            ))}
          </ul>
        )}

        {saveError && <p className="input-error" role="alert">{saveError}</p>}
        {savedMessage && <p className={styles.valid} role="status">{savedMessage}</p>}
        <button className="button" type="button" disabled={!canSave} onClick={() => void save()}>
          {saving ? "Saving…" : "Save deck"}
        </button>
        <Link className="text-link" href="/decks">Back to your decks</Link>
      </aside>

      {selected && <CardDetailDialog card={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
