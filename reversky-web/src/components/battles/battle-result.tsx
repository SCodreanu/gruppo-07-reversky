"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { CardFace, cardFrameClass } from "@/components/cards/card-tile";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { BattleDetail } from "@/lib/api/contracts";
import { finalLife, shownLife, type Side, type Timeline } from "@/lib/battle/timeline";
import { playerAvatar } from "@/lib/profile-avatar";
import { modeLabel, useBattle, winnerLabel, type CardLookup } from "./use-battle";
import styles from "./play.module.css";

const END_REASONS: Record<string, string> = {
  LIFE_POINTS_ZERO: "Decisive blow",
  MAX_ROUNDS: "Round limit reached",
  ROUND_LIMIT: "Round limit reached",
  DECK_EMPTY: "No creatures left",
};

function endTitle(reason: string | null): string {
  if (!reason) return "Match over";
  return END_REASONS[reason] ?? reason.replaceAll("_", " ").toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
}

/** The winner's creature on the table at the end (for a draw, the viewer's). */
function decidingCard(battle: BattleDetail, timeline: Timeline, cards: CardLookup) {
  const side: Side = battle.outcome === "LOSS" ? "opponent" : "me";
  for (let index = timeline.phases.length - 1; index >= 0; index -= 1) {
    const name = timeline.phases[index].board[side].creature?.name;
    if (name) return cards[side].get(name) ?? cards.any.get(name) ?? null;
  }
  return null;
}

/** What decided the match: the last points phase and the final event. */
function summaryLines(timeline: Timeline): string[] {
  const lastPoints = [...timeline.phases].reverse().find((phase) => phase.kind === "points");
  const end = timeline.phases.find((phase) => phase.kind === "end");
  return [...(lastPoints?.events ?? []), ...(end?.events ?? [])].map((event) => event.message ?? "").filter(Boolean).slice(-4);
}

function RetryPanel({ battle }: { battle: BattleDetail }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function retry() {
    setPending(true);
    setError("");
    const result = await gameFetch<BattleDetail>(`battles/${battle.id}/retry`, { method: "POST" });
    setPending(false);
    if (result.ok) router.push(`/battles/${battle.id}/play`);
    else setError(problemMessage(result.problem, "The engine is still unavailable. Try again in a moment."));
  }
  return (
    <div className={styles.column}>
      <p className="eyebrow">{modeLabel(battle.mode)}</p>
      <h1 className="display-type">The battle hasn’t been played yet.</h1>
      <p className={styles.note}>The simulation engine didn’t answer. The decks and the seed are saved: retrying plays the same battle.</p>
      {error && <p className={`${styles.feedback} ${styles.feedbackError}`} role="alert">{error}</p>}
      <div className={styles.links}>
        <button className="button" type="button" onClick={() => void retry()} disabled={pending}>{pending ? "Simulating…" : "Retry the battle"}</button>
        <Link className="button button--quiet" href="/battles">Back to Play</Link>
      </div>
    </div>
  );
}

export function BattleResult({ battleId }: { battleId: string }) {
  const load = useBattle(battleId, `/battles/${battleId}`);

  if (load.status === "loading") return <p className={styles.note} aria-live="polite">Loading the result…</p>;
  if (load.status === "error") {
    return (
      <div className={styles.column} role="alert">
        <p className={styles.note}>{load.message}</p>
        <Link className="button button--quiet" href="/battles">Back to Play</Link>
      </div>
    );
  }

  const { battle, timeline, cards } = load;
  if (!battle.completed) return <RetryPanel battle={battle} />;

  const card = decidingCard(battle, timeline, cards);
  const life = finalLife(battle, timeline);
  const players = [
    { name: `${battle.me.username} · You`, avatar: playerAvatar(battle.me.id), life: life.me },
    { name: battle.opponent.username, avatar: playerAvatar(battle.opponent.id), life: life.opponent },
  ];

  return (
    <section className={styles.result} aria-labelledby="result-title">
      <div className={styles.resultCard}>
        {card && (
          <div className={cardFrameClass(card)}>
            <CardFace card={card} sizes="240px" priority />
          </div>
        )}
      </div>
      <div className={styles.resultBody}>
        <p>Match over · {modeLabel(battle.mode)} · {timeline.roundStarts.length} rounds</p>
        <h1 className="display-type" id="result-title">{winnerLabel(battle)}</h1>
        <h2 className="display-type">{endTitle(battle.endReason)}</h2>
        <ul className={styles.summary}>
          {summaryLines(timeline).map((line, index) => <li key={index}>{line}</li>)}
        </ul>
        {battle.mode === "RANKED" && battle.ratingBefore !== null && battle.ratingDelta !== null && (
          <p className={styles.ratingChange}>
            Rating {battle.ratingBefore} → {battle.ratingBefore + battle.ratingDelta}
            <strong className={battle.ratingDelta >= 0 ? styles.win : styles.loss}>{battle.ratingDelta >= 0 ? "+" : ""}{battle.ratingDelta}</strong>
          </p>
        )}
        <div className={styles.players}>
          {players.map((player) => (
            <div className={styles.playerChip} key={player.name}>
              <ProfileAvatar index={player.avatar} className={styles.chipAvatar} />
              <span>
                <strong>{player.name}</strong>
                <small>PV {shownLife(player.life)}</small>
              </span>
            </div>
          ))}
        </div>
        <div className={styles.links}>
          <Link className="button" href="/battles">Back to Play</Link>
          <Link className="button button--quiet" href="/battles/history">History</Link>
          <Link className="button button--quiet" href={`/battles/${battle.id}/replay`}>Watch again</Link>
        </div>
      </div>
    </section>
  );
}
