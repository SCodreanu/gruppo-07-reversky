"use client";

import { useState } from "react";
import { CardDetailDialog } from "@/components/cards/card-detail-dialog";
import { CardFilters, EMPTY_FILTERS, Pager, useReferenceData, type CardFilterValues } from "@/components/cards/card-filters";
import { CardTile } from "@/components/cards/card-tile";
import { usePaged } from "@/components/cards/use-paged";
import { queryString } from "@/lib/api/browser";
import type { CatalogCard } from "@/lib/api/contracts";
import styles from "@/components/cards/cards.module.css";

const PAGE_SIZE = 24;

export function CatalogBrowser() {
  const { rarities, factions } = useReferenceData();
  const [filters, setFilters] = useState<CardFilterValues>(EMPTY_FILTERS);
  const [pageIndex, setPageIndex] = useState(0);
  const [selected, setSelected] = useState<CatalogCard | null>(null);

  const query = queryString({
    name: filters.name.trim(),
    type: filters.type,
    rarityId: filters.rarityId,
    factionId: filters.factionId,
    page: pageIndex,
    size: PAGE_SIZE,
  });
  const { page, error, loading, reload } = usePaged<CatalogCard>("cards", query, "/catalog");

  const resultLabel = page ? `${page.totalElements} ${page.totalElements === 1 ? "card" : "cards"}${loading ? " · updating…" : ""}` : "Loading the catalog…";

  return (
    <>
      <CardFilters
        values={filters}
        onChange={(values) => {
          setFilters(values);
          setPageIndex(0);
        }}
        rarities={rarities}
        factions={factions}
        resultLabel={resultLabel}
      />

      {error && (
        <div className={styles.state} role="alert">
          <p>{error}</p>
          <button className="button button--quiet" type="button" onClick={reload}>Try again</button>
        </div>
      )}

      {page && page.content.length === 0 && !loading && !error && (
        <div className={styles.state}><p>No card matches these filters.</p></div>
      )}

      {page && page.content.length > 0 && (
        <div className={styles.grid} aria-busy={loading}>
          {page.content.map((card) => <CardTile key={card.id} card={card} onSelect={setSelected} />)}
        </div>
      )}

      {page && <Pager page={page.page} totalPages={page.totalPages} onPage={setPageIndex} />}

      {selected && <CardDetailDialog card={selected} onClose={() => setSelected(null)} />}
    </>
  );
}
