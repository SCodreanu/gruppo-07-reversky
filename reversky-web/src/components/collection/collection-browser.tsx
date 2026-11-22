"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { CardDetailDialog } from "@/components/cards/card-detail-dialog";
import { CardFilters, EMPTY_FILTERS, Pager, useReferenceData, type CardFilterValues } from "@/components/cards/card-filters";
import { rarityTone } from "@/components/cards/card-meta";
import { CardTile } from "@/components/cards/card-tile";
import { usePaged } from "@/components/cards/use-paged";
import { gameFetch, queryString } from "@/lib/api/browser";
import type { CollectionEntry, CollectionSummary } from "@/lib/api/contracts";
import cardStyles from "@/components/cards/cards.module.css";
import styles from "./collection.module.css";

const PAGE_SIZE = 24;
const SORT_OPTIONS = [
  { value: "name,asc", label: "Name" },
  { value: "rarity,desc", label: "Rarest first" },
  { value: "rarity,asc", label: "Most common first" },
  { value: "quantity,desc", label: "Most copies" },
  { value: "type,asc", label: "Type" },
];

function SummaryPanel() {
  const [summary, setSummary] = useState<CollectionSummary | null>(null);
  useEffect(() => {
    let active = true;
    void gameFetch<CollectionSummary>("collection/summary").then((result) => {
      if (active && result.ok) setSummary(result.data);
    });
    return () => {
      active = false;
    };
  }, []);

  if (!summary) return null;
  const percent = summary.total ? Math.round((summary.owned / summary.total) * 100) : 0;
  return (
    <section className={styles.summary} aria-labelledby="collection-progress">
      <div className={styles.summaryHead}>
        <div>
          <p className="eyebrow">Catalog progress</p>
          <h2 id="collection-progress" className="display-type">{summary.owned} of {summary.total} cards</h2>
        </div>
        <strong className={styles.percent}>{percent}%</strong>
      </div>
      <ul className={styles.rarities}>
        {summary.rarities.map((row) => (
          <li key={row.rarity.id} className={cardStyles[`tone-${rarityTone(row.rarity.name)}`]}>
            <span className={styles.rarityName}>{row.rarity.name}</span>
            <span className={styles.bar} aria-hidden="true">
              <span style={{ width: `${row.total ? (row.owned / row.total) * 100 : 0}%` }} />
            </span>
            <span className={styles.rarityCount}>{row.owned}/{row.total}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function CollectionBrowser() {
  const { rarities, factions } = useReferenceData();
  const [filters, setFilters] = useState<CardFilterValues>(EMPTY_FILTERS);
  const [sort, setSort] = useState(SORT_OPTIONS[0].value);
  const [pageIndex, setPageIndex] = useState(0);
  const [selected, setSelected] = useState<CollectionEntry | null>(null);

  const query = queryString({
    name: filters.name.trim(),
    type: filters.type,
    rarityId: filters.rarityId,
    factionId: filters.factionId,
    sort,
    page: pageIndex,
    size: PAGE_SIZE,
  });
  const { page, error, loading, reload } = usePaged<CollectionEntry>("collection", query, "/collection");
  const unfiltered = filters === EMPTY_FILTERS;

  return (
    <>
      <SummaryPanel />

      <CardFilters
        values={filters}
        onChange={(values) => {
          setFilters(values);
          setPageIndex(0);
        }}
        rarities={rarities}
        factions={factions}
        sort={sort}
        sortOptions={SORT_OPTIONS}
        onSortChange={(value) => {
          setSort(value);
          setPageIndex(0);
        }}
        resultLabel={page ? `${page.totalElements} different ${page.totalElements === 1 ? "card" : "cards"}${loading ? " · updating…" : ""}` : "Loading your collection…"}
      />

      {error && (
        <div className={cardStyles.state} role="alert">
          <p>{error}</p>
          <button className="button button--quiet" type="button" onClick={reload}>Try again</button>
        </div>
      )}

      {page && page.content.length === 0 && !loading && !error && (
        <div className={cardStyles.state}>
          {unfiltered ? (
            <>
              <p>Your collection is empty. Open a pack to find your first cards.</p>
              <Link className="button" href="/shop">Go to the shop</Link>
            </>
          ) : <p>No card in your collection matches these filters.</p>}
        </div>
      )}

      {page && page.content.length > 0 && (
        <div className={cardStyles.grid} aria-busy={loading}>
          {page.content.map((entry) => (
            <CardTile
              key={entry.card.id}
              card={entry.card}
              badge={`×${entry.quantity}`}
              onSelect={() => setSelected(entry)}
              footer={(
                <>
                  <span>{entry.usedInDecks ? `In ${entry.usedInDecks} ${entry.usedInDecks === 1 ? "deck" : "decks"}` : "Not in a deck"}</span>
                  {entry.reservedQuantity > 0 && <span title="Offered in an open trade">{entry.reservedQuantity} reserved</span>}
                </>
              )}
            />
          ))}
        </div>
      )}

      {page && <Pager page={page.page} totalPages={page.totalPages} onPage={setPageIndex} />}

      {selected && (
        <CardDetailDialog card={selected.card} onClose={() => setSelected(null)}>
          <dl className={styles.ownership}>
            <div><dt>Copies owned</dt><dd>{selected.quantity}</dd></div>
            <div><dt>Reserved in trades</dt><dd>{selected.reservedQuantity}</dd></div>
            <div><dt>Used in decks</dt><dd>{selected.usedInDecks}</dd></div>
          </dl>
        </CardDetailDialog>
      )}
    </>
  );
}
