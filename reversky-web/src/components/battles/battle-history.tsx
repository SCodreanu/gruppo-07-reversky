"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import { gameFetch, problemMessage, queryString } from "@/lib/api/browser";
import type { BattleSummary, PageResponse } from "@/lib/api/contracts";
import { shownLife } from "@/lib/battle/timeline";
import { playerAvatar } from "@/lib/profile-avatar";
import styles from "./play.module.css";

const PAGE_SIZE = 10;

function outcomeText(battle: BattleSummary): string {
  if (!battle.completed) return "Not played yet";
  const life = (value: number | null) => (value === null ? "?" : shownLife(value));
  const score = `${life(battle.myFinalLifePoints)} PV to ${life(battle.opponentFinalLifePoints)}`;
  if (battle.outcome === "WIN") return `Victory · ${score}`;
  if (battle.outcome === "LOSS") return `Defeat · ${score}`;
  return `Draw · ${score}`;
}

function dateLabel(value: string): string {
  return new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

/** Match history (mockup "Storico partite"): each match opens its result or its replay on the table. */
export function BattleHistory() {
  const router = useRouter();
  const [battles, setBattles] = useState<BattleSummary[]>([]);
  const [page, setPage] = useState<PageResponse<BattleSummary> | null>(null);
  const [pageIndex, setPageIndex] = useState(0);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void gameFetch<PageResponse<BattleSummary>>(`battles${queryString({ page: pageIndex, size: PAGE_SIZE })}`).then((result) => {
      if (!active) return;
      if (!result.ok && result.status === 401) {
        router.replace(`/login?next=${encodeURIComponent("/battles/history")}`);
        return;
      }
      if (!result.ok) {
        setError(problemMessage(result.problem, "We couldn’t load your matches."));
        return;
      }
      setPage(result.data);
      setBattles((current) => (pageIndex === 0 ? result.data.content : [...current, ...result.data.content]));
    });
    return () => {
      active = false;
    };
  }, [pageIndex, router]);

  return (
    <div className={styles.column}>
      {error && <p className={`${styles.feedback} ${styles.feedbackError}`} role="alert">{error}</p>}
      {!page && !error && <p className={styles.note} aria-live="polite">Loading your matches…</p>}
      {page && battles.length === 0 && <p className={styles.note}>No matches yet. Challenge a friend or try a ranked match.</p>}
      {battles.map((battle) => (
        <div className={styles.row} key={battle.battleId}>
          <ProfileAvatar index={playerAvatar(battle.opponent.id)} className={styles.rowAvatar} />
          <span className={styles.rowText}>
            <strong>vs {battle.opponent.username}</strong>
            <span className={battle.outcome === "WIN" ? styles.win : battle.outcome === "LOSS" ? styles.loss : ""}>{outcomeText(battle)}</span>
            <span>
              {battle.mode === "RANKED" ? "Ranked" : "Friendly"}
              {battle.rounds ? ` · ${battle.rounds} rounds` : ""}
              {battle.ratingDelta !== null ? ` · ${battle.ratingDelta >= 0 ? "+" : ""}${battle.ratingDelta} rating` : ""}
              {` · ${dateLabel(battle.createdAt)}`}
            </span>
          </span>
          <span className={styles.rowActions}>
            <Link className="button button--quiet" href={`/battles/${battle.battleId}`}>Result</Link>
            {battle.completed && <Link className="button button--quiet" href={`/battles/${battle.battleId}/replay`}>Replay</Link>}
          </span>
        </div>
      ))}
      {page && page.page + 1 < page.totalPages && (
        <button className="button button--quiet" type="button" onClick={() => setPageIndex((value) => value + 1)}>Load more</button>
      )}
      <div className={styles.links}>
        <Link className="button button--quiet" href="/battles">Back to Play</Link>
      </div>
    </div>
  );
}
