"use client";

import { useEffect, useState } from "react";
import { gameFetch } from "@/lib/api/browser";
import type { CardType, Faction, Rarity } from "@/lib/api/contracts";
import { CARD_TYPES, shortFactionName, typeLabel } from "./card-meta";
import styles from "./cards.module.css";

export type CardFilterValues = {
  name: string;
  type: CardType | "";
  rarityId: string;
  factionId: string;
};

export const EMPTY_FILTERS: CardFilterValues = { name: "", type: "", rarityId: "", factionId: "" };

export type SortOption = { value: string; label: string };

/** Factions and rarities for the filter selects, read once per page. */
export function useReferenceData() {
  const [rarities, setRarities] = useState<Rarity[]>([]);
  const [factions, setFactions] = useState<Faction[]>([]);
  useEffect(() => {
    let active = true;
    void Promise.all([gameFetch<Rarity[]>("rarities"), gameFetch<Faction[]>("factions")]).then(([rarityResult, factionResult]) => {
      if (!active) return;
      if (rarityResult.ok) setRarities([...rarityResult.data].sort((a, b) => a.sortingOrder - b.sortingOrder));
      if (factionResult.ok) setFactions(factionResult.data);
    });
    return () => {
      active = false;
    };
  }, []);
  return { rarities, factions };
}

export function CardFilters({
  values,
  onChange,
  rarities,
  factions,
  sort,
  sortOptions,
  onSortChange,
  resultLabel,
}: {
  values: CardFilterValues;
  onChange: (values: CardFilterValues) => void;
  rarities: Rarity[];
  factions: Faction[];
  sort?: string;
  sortOptions?: SortOption[];
  onSortChange?: (value: string) => void;
  resultLabel?: string;
}) {
  const filtered = values.name !== "" || values.type !== "" || values.rarityId !== "" || values.factionId !== "";
  return (
    <div className={styles.filters} role="search">
      <label className={styles.filterField}>
        <span className="field-label">Name</span>
        <input
          className="text-input"
          type="search"
          placeholder="Search a card"
          value={values.name}
          onChange={(event) => onChange({ ...values, name: event.target.value })}
        />
      </label>
      <label className={styles.filterField}>
        <span className="field-label">Type</span>
        <select className="text-input" value={values.type} onChange={(event) => onChange({ ...values, type: event.target.value as CardType | "" })}>
          <option value="">All types</option>
          {CARD_TYPES.map((type) => <option key={type} value={type}>{typeLabel(type)}</option>)}
        </select>
      </label>
      <label className={styles.filterField}>
        <span className="field-label">Rarity</span>
        <select className="text-input" value={values.rarityId} onChange={(event) => onChange({ ...values, rarityId: event.target.value })}>
          <option value="">All rarities</option>
          {rarities.map((rarity) => <option key={rarity.id} value={rarity.id}>{rarity.name}</option>)}
        </select>
      </label>
      <label className={styles.filterField}>
        <span className="field-label">Theory</span>
        <select className="text-input" value={values.factionId} onChange={(event) => onChange({ ...values, factionId: event.target.value })}>
          <option value="">All Theories</option>
          {factions.map((faction) => <option key={faction.id} value={faction.id}>{shortFactionName(faction.name)}</option>)}
        </select>
      </label>
      {sortOptions && onSortChange && (
        <label className={styles.filterField}>
          <span className="field-label">Sort</span>
          <select className="text-input" value={sort} onChange={(event) => onSortChange(event.target.value)}>
            {sortOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
      )}
      <div className={styles.filterSummary}>
        {resultLabel && <span>{resultLabel}</span>}
        {filtered && (
          <button className={styles.clearFilters} type="button" onClick={() => onChange(EMPTY_FILTERS)}>Clear filters</button>
        )}
      </div>
    </div>
  );
}

export function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className={styles.pager} aria-label="Pages">
      <button className="button button--quiet" type="button" disabled={page <= 0} onClick={() => onPage(page - 1)}>‹ Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="button button--quiet" type="button" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>Next ›</button>
    </nav>
  );
}
