"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { BattleDetail, BattleEvent } from "@/lib/api/contracts";
import styles from "./battle-result.module.css";

function dateLabel(value: string): string {
  return new Intl.DateTimeFormat("en", { dateStyle: "long", timeStyle: "short" }).format(new Date(value));
}

function resultName(outcome: BattleDetail["outcome"]): string {
  if (outcome === "WIN") return "Victory";
  if (outcome === "LOSS") return "Defeat";
  if (outcome === "DRAW") return "Draw";
  return "Battle report";
}

function eventText(event: BattleEvent): string {
  return event.message || event.type?.replaceAll("_", " ").toLowerCase() || "Battle event";
}

function numberValue(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function stringValue(value: unknown): string | null {
  return typeof value === "string" ? value : null;
}

type FieldCreature = { name: string; attack: number | null; defense: number | null };
type PlayerField = {
  lifePoints: number;
  creature: FieldCreature | null;
  terrain: string | null;
  item: string | null;
  hidden: number;
};

function replayBoard(battle: BattleDetail, events: BattleEvent[], throughRound: number): { me: PlayerField; opponent: PlayerField } {
  const opening = events.find((event) => event.type === "MATCH_STARTED");
  const startingLife = numberValue(opening?.data?.startingLifePoints) ?? 30;
  const initialField = (): PlayerField => ({
    lifePoints: startingLife,
    creature: null,
    terrain: null,
    item: null,
    hidden: 0,
  });
  const field = { me: initialField(), opponent: initialField() };
  const owner = (side: BattleEvent["side"]): "me" | "opponent" | null => {
    if (!side) return null;
    return side === battle.mySide ? "me" : "opponent";
  };

  for (const event of events) {
    if ((event.round ?? 0) > throughRound) continue;
    const side = owner(event.side);
    const data = event.data ?? {};
    const card = stringValue(data.card);

    if (event.type === "PLAYER_DAMAGED" && side) {
      field[side].lifePoints = numberValue(data.lifePoints) ?? field[side].lifePoints;
    } else if (event.type === "CREATURE_ENTERED" && side && card) {
      field[side].creature = {
        name: card,
        attack: numberValue(data.attack),
        defense: numberValue(data.defense),
      };
    } else if (event.type === "ATTACK_COMPUTED" && side && field[side].creature) {
      const attacker = stringValue(data.attacker);
      if (field[side].creature?.name === attacker) {
        field[side].creature.attack = numberValue(data.total) ?? field[side].creature.attack;
      }
    } else if (event.type === "DAMAGE_DEALT" && side) {
      const targetSide = side === "me" ? "opponent" : "me";
      const target = stringValue(data.target);
      if (field[targetSide].creature?.name === target) {
        field[targetSide].creature.defense = numberValue(data.targetDefense) ?? field[targetSide].creature.defense;
      }
    } else if (event.type === "CREATURE_DIED" && side && card === field[side].creature?.name) {
      field[side].creature = null;
    } else if (event.type === "TERRAIN_PLACED" && side && card) {
      field[side].terrain = card;
    } else if (event.type === "TERRAIN_REMOVED" && side && card === field[side].terrain) {
      field[side].terrain = null;
    } else if (event.type === "ITEM_PLACED" && side && card) {
      field[side].item = card;
    } else if (event.type === "ITEM_CONSUMED" && side && card === field[side].item) {
      field[side].item = null;
    } else if (event.type === "HIDDEN_SET" && side) {
      field[side].hidden += 1;
    } else if (event.type === "HIDDEN_TRIGGERED" && side) {
      field[side].hidden = Math.max(0, field[side].hidden - 1);
    }
  }

  return field;
}

export function BattleResult({ battleId }: { battleId: string }) {
  const router = useRouter();
  const [battle, setBattle] = useState<BattleDetail | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(false);
  const [currentRound, setCurrentRound] = useState(1);

  useEffect(() => {
    let active = true;
    void gameFetch<BattleDetail>(`battles/${battleId}`).then((result) => {
      if (!active) return;
      if (result.ok) setBattle(result.data);
      else if (result.status === 401) router.replace(`/login?next=${encodeURIComponent(`/battles/${battleId}`)}`);
      else setError(problemMessage(result.problem, "We couldn’t load this battle."));
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [battleId, router]);

  const rounds = useMemo(() => {
    const grouped = new Map<number, BattleEvent[]>();
    for (const event of battle?.log ?? []) {
      const round = event.round ?? 0;
      const list = grouped.get(round) ?? [];
      list.push(event);
      grouped.set(round, list);
    }
    return grouped;
  }, [battle?.log]);

  const totalRounds = Math.max(battle?.totalRounds ?? 0, ...rounds.keys());
  const replayRound = totalRounds === 0 ? 0 : Math.min(currentRound, totalRounds);
  const roundEvents = rounds.get(replayRound) ?? [];
  const board = useMemo(
    () => battle ? replayBoard(battle, battle.log, replayRound) : null,
    [battle, replayRound],
  );

  async function retryBattle() {
    setRetrying(true);
    setError("");
    const result = await gameFetch<BattleDetail>(`battles/${battleId}/retry`, { method: "POST" });
    setRetrying(false);
    if (result.ok) setBattle(result.data);
    else if (result.status === 401) router.replace(`/login?next=${encodeURIComponent(`/battles/${battleId}`)}`);
    else setError(problemMessage(result.problem, "The battle could not be completed yet. Try again shortly."));
  }

  if (loading) return <div className={styles.state} aria-live="polite"><span aria-hidden="true">✦</span><p>Opening the battle log…</p></div>;
  if (error && !battle) {
    return (
      <div className={styles.state} role="alert">
        <p>{error}</p>
        <Link className="button button--quiet" href="/battles">Back to Play</Link>
      </div>
    );
  }
  if (!battle) return null;

  const resultTone = battle.outcome === "WIN" ? styles.resultWin : battle.outcome === "LOSS" ? styles.resultLoss : styles.resultDraw;

  return (
    <div className={styles.resultPage}>
      <Link className={styles.backLink} href="/battles">← Back to Play</Link>
      {error && <p className={styles.error} role="alert">{error}</p>}

      <section className={`${styles.resultBanner} ${resultTone}`}>
        <p className="eyebrow">{battle.mode === "RANKED" ? "RANKED MATCH" : "FRIENDLY CHALLENGE"} / {dateLabel(battle.createdAt)}</p>
        <h2 className="display-type">{battle.completed ? resultName(battle.outcome) : "The crossing is unfinished"}</h2>
        <p>{battle.completed ? `Against ${battle.opponent.username}` : "The result is saved and can be simulated again with the same decks and rules."}</p>
        {battle.mode === "RANKED" && battle.ratingDelta !== null && (
          <span className={styles.ratingDelta}>{battle.ratingDelta > 0 ? "+" : ""}{battle.ratingDelta} rating</span>
        )}
      </section>

      {board && (
        <section className={styles.arenaPanel} aria-labelledby="battle-table-title">
          <div className={styles.arenaHeading}>
            <div>
              <p className="eyebrow">G06 / ROUND BY ROUND</p>
              <h3 id="battle-table-title" className="display-type">The battle table</h3>
              <p>Follow the match one round at a time.</p>
            </div>
            <div className={styles.roundControls} aria-label="Battle replay controls">
              <button type="button" onClick={() => setCurrentRound((round) => Math.max(1, round - 1))} disabled={replayRound <= 1} aria-label="Previous round">←</button>
              <span>{totalRounds === 0 ? "Opening" : `Round ${replayRound} / ${totalRounds}`}</span>
              <button type="button" onClick={() => setCurrentRound((round) => Math.min(totalRounds, round + 1))} disabled={totalRounds === 0 || replayRound >= totalRounds} aria-label="Next round">→</button>
            </div>
          </div>
          {totalRounds > 1 && (
            <label className={styles.roundSlider}>
              <span className="muted">Jump to round</span>
              <input type="range" min="1" max={totalRounds} value={replayRound} onChange={(event) => setCurrentRound(Number(event.target.value))} />
            </label>
          )}
          <div className={styles.tableSurface} aria-live="polite">
            <ArenaPlayer side="OPPONENT" name={battle.opponent.username} field={board.opponent} startingLife={numberValue(eventsOpening(battle.log)?.data?.startingLifePoints) ?? 30} />
            <div className={styles.tableDivider}><span>{replayRound > 0 ? `ROUND ${replayRound}` : "THE CROSSING"}</span><i /><strong>VS</strong><i /><span>{battle.rulesVersion}</span></div>
            <ArenaPlayer side="YOU" name={battle.me.username} field={board.me} startingLife={numberValue(eventsOpening(battle.log)?.data?.startingLifePoints) ?? 30} />
            <p className={styles.tableCaption}>{roundEvents.at(-1) ? eventText(roundEvents.at(-1)!) : "The creatures are ready. Advance to begin the round."}</p>
          </div>
          <div className={styles.roundLog}>
            <div className={styles.roundLogHeading}>
              <h4 className="display-type">{replayRound > 0 ? `Round ${replayRound}` : "Opening"}</h4>
              <span>{roundEvents.length} {roundEvents.length === 1 ? "event" : "events"}</span>
            </div>
            {roundEvents.length === 0 ? <p className={styles.noLog}>No events were recorded for this round.</p> : (
              <ol>
                {roundEvents.map((event, index) => (
                  <li key={`${event.sequence ?? replayRound}-${index}`}>
                    <span className={styles.eventDot} aria-hidden="true" />
                    <span>{eventText(event)}</span>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </section>
      )}

      {!battle.completed && (
        <section className={styles.retryPanel}>
          <div>
            <strong>Battle simulation needs another try</strong>
            <p>The saved decks and seed stay the same when you retry.</p>
          </div>
          <button className="button" type="button" onClick={() => void retryBattle()} disabled={retrying}>
            {retrying ? "Simulating…" : "Retry simulation"}
          </button>
        </section>
      )}

      <section className={styles.scoreGrid} aria-label="Battle summary">
        <article className={styles.playerCard}>
          <span className="eyebrow">YOU</span>
          <h3 className="display-type">{battle.me.username}</h3>
          <div className={styles.playerStats}>
            <span><small>FINAL LIFE</small><strong>{battle.me.finalLifePoints ?? "—"}</strong></span>
            <span><small>CREATURES DEFEATED</small><strong>{battle.me.creaturesDefeated ?? "—"}</strong></span>
          </div>
        </article>
        <div className={styles.versus} aria-hidden="true">VS</div>
        <article className={styles.playerCard}>
          <span className="eyebrow">OPPONENT</span>
          <h3 className="display-type">{battle.opponent.username}</h3>
          <div className={styles.playerStats}>
            <span><small>FINAL LIFE</small><strong>{battle.opponent.finalLifePoints ?? "—"}</strong></span>
            <span><small>CREATURES DEFEATED</small><strong>{battle.opponent.creaturesDefeated ?? "—"}</strong></span>
          </div>
        </article>
      </section>

      <div className={styles.resultActions}>
        <Link className="button" href="/battles">Return to Play</Link>
      </div>
    </div>
  );
}

function eventsOpening(events: BattleEvent[]): BattleEvent | undefined {
  return events.find((event) => event.type === "MATCH_STARTED");
}

function ArenaPlayer({ side, name, field, startingLife }: { side: string; name: string; field: PlayerField; startingLife: number }) {
  const lifePercent = Math.max(0, Math.min(100, (field.lifePoints / startingLife) * 100));
  return (
    <article className={side === "YOU" ? styles.arenaPlayer : `${styles.arenaPlayer} ${styles.arenaOpponent}`}>
      <div className={styles.arenaIdentity}>
        <span className={styles.playerSigil} aria-hidden="true">{name.slice(0, 1).toUpperCase()}</span>
        <span><small>{side}</small><strong>{name}</strong></span>
        <span className={styles.lifePoints}><small>LIFE</small><strong>{field.lifePoints}</strong></span>
        <span className={styles.lifeTrack} aria-hidden="true"><i style={{ width: `${lifePercent}%` }} /></span>
      </div>
      <div className={styles.creatureSlot}>
        {field.creature ? (
          <>
            <span className={styles.cardLabel}>ACTIVE CREATURE</span>
            <strong className="display-type">{field.creature.name}</strong>
            <span className={styles.creatureStats}><span>ATK <b>{field.creature.attack ?? "—"}</b></span><span>DEF <b>{field.creature.defense ?? "—"}</b></span></span>
          </>
        ) : (
          <><span className={styles.emptyCardMark} aria-hidden="true">✧</span><span className={styles.emptySlotText}>No creature in play</span></>
        )}
      </div>
      <div className={styles.fieldTokens}>
        <span><small>TERRAIN</small><strong>{field.terrain ?? "—"}</strong></span>
        <span><small>ITEM</small><strong>{field.item ?? "—"}</strong></span>
        <span><small>HIDDEN</small><strong>{field.hidden > 0 ? `${field.hidden} set` : "—"}</strong></span>
      </div>
    </article>
  );
}
