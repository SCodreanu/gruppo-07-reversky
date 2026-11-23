"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { BattleDetail, CatalogCard, PageResponse } from "@/lib/api/contracts";
import { buildTimeline, type Side, type Timeline } from "@/lib/battle/timeline";

/** Catalog cards of both decks, by name: the battle log names cards, the deck snapshots give their ids. */
export type CardLookup = Record<Side, Map<string, CatalogCard>> & { any: Map<string, CatalogCard> };

type DeckEntry = { cardId?: number; name?: string };

let catalogPromise: Promise<Map<number, CatalogCard>> | null = null;

/** The whole public catalog (two pages of 100), read once per tab. */
function loadCatalog(): Promise<Map<number, CatalogCard>> {
  catalogPromise ??= (async () => {
    const cards = new Map<number, CatalogCard>();
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const result = await gameFetch<PageResponse<CatalogCard>>(`cards?page=${page}&size=100`);
      if (!result.ok) break;
      result.data.content.forEach((card) => cards.set(card.id, card));
      totalPages = result.data.totalPages;
      page += 1;
    }
    if (!cards.size) catalogPromise = null;
    return cards;
  })();
  return catalogPromise;
}

function lookupFor(deck: unknown[], catalog: Map<number, CatalogCard>): Map<string, CatalogCard> {
  const byName = new Map<string, CatalogCard>();
  for (const entry of deck as DeckEntry[]) {
    const card = typeof entry.cardId === "number" ? catalog.get(entry.cardId) : undefined;
    if (card && entry.name) byName.set(entry.name, card);
  }
  return byName;
}

export type BattleLoad =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; battle: BattleDetail; timeline: Timeline; cards: CardLookup };

export function useBattle(battleId: string, loginNext: string): BattleLoad {
  const router = useRouter();
  const [state, setState] = useState<
    { status: "loading" } | { status: "error"; message: string } | { status: "loaded"; battle: BattleDetail; catalog: Map<number, CatalogCard> }
  >({ status: "loading" });

  useEffect(() => {
    let active = true;
    void Promise.all([gameFetch<BattleDetail>(`battles/${battleId}`), loadCatalog()]).then(([result, catalog]) => {
      if (!active) return;
      if (!result.ok && result.status === 401) {
        router.replace(`/login?next=${encodeURIComponent(loginNext)}`);
        return;
      }
      if (!result.ok) {
        setState({ status: "error", message: result.status === 404 ? "This battle doesn’t exist or isn’t yours." : problemMessage(result.problem, "We couldn’t load this battle.") });
        return;
      }
      setState({ status: "loaded", battle: result.data, catalog });
    });
    return () => {
      active = false;
    };
  }, [battleId, loginNext, router]);

  return useMemo<BattleLoad>(() => {
    if (state.status !== "loaded") return state;
    const { battle, catalog } = state;
    const me = lookupFor(battle.me.deck, catalog);
    const opponent = lookupFor(battle.opponent.deck, catalog);
    const any = new Map([...opponent, ...me]);
    return { status: "ready", battle, timeline: buildTimeline(battle), cards: { me, opponent, any } };
  }, [state]);
}

export function modeLabel(mode: BattleDetail["mode"]): string {
  return mode === "RANKED" ? "Ranked match" : "Friendly challenge";
}

/** "alice wins", "bob wins" or "Draw", from the viewer's side. */
export function winnerLabel(battle: BattleDetail): string {
  if (battle.outcome === "DRAW") return "Draw";
  if (battle.outcome === "WIN") return `${battle.me.username} wins`;
  if (battle.outcome === "LOSS") return `${battle.opponent.username} wins`;
  return "Battle not finished";
}
