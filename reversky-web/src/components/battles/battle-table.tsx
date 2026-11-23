"use client";

import Image from "next/image";
import Link from "next/link";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Brand } from "@/components/brand-mark";
import { CardDetailDialog } from "@/components/cards/card-detail-dialog";
import { CardFace, cardFrameClass } from "@/components/cards/card-tile";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import type { BattleDetail, CatalogCard } from "@/lib/api/contracts";
import { shownLife, type Badge, type FieldCreature, type Phase, type Side, type SideState, type Timeline } from "@/lib/battle/timeline";
import { modeLabel, winnerLabel, type CardLookup } from "./use-battle";
import styles from "./battle-table.module.css";

/** How long each phase stays on the table while playing. */
const PHASE_MS: Record<Phase["kind"], number> = {
  deploy: 1700,
  effects: 1500,
  battle: 1900,
  after: 1500,
  points: 1700,
  end: 1500,
};

type Player = { name: string; avatar: number; you: boolean };

type Inspected = { card: CatalogCard; creature?: FieldCreature; label?: string };

const HISTORY_LINES = 8;

/** What a card has done so far: the log lines up to the current phase that name it. */
function cardHistory(timeline: Timeline, index: number, name: string): { round: number; message: string }[] {
  const lines: { round: number; message: string }[] = [];
  for (const phase of timeline.phases.slice(0, index + 1)) {
    for (const event of phase.events) {
      if (event.message?.includes(name)) lines.push({ round: phase.round, message: event.message });
    }
  }
  return lines;
}

/** Opens the detail of a card on the table (the battle pauses meanwhile). */
const InspectContext = createContext<(inspected: Inspected) => void>(() => {});

function TableCard({
  name,
  cards,
  className = "",
  creature,
  faceDown = false,
  label,
}: {
  name: string | null;
  cards: Map<string, CatalogCard>;
  className?: string;
  creature?: FieldCreature;
  faceDown?: boolean;
  label?: string;
}) {
  const inspect = useContext(InspectContext);
  if (faceDown) {
    return (
      <figure className={`${styles.slot} ${className}`}>
        <span className={styles.cardBack}><Image src="/images/reversky-card-back.png" alt="" fill sizes="120px" /></span>
        {label && <figcaption>{label}</figcaption>}
      </figure>
    );
  }
  const card = name ? cards.get(name) : undefined;
  return (
    <figure className={`${styles.slot} ${className}`}>
      {card ? (
        <button
          className={cardFrameClass(card, `${styles.tableCard} ${styles.inspectable}`)}
          type="button"
          onClick={() => inspect({ card, creature, label })}
          aria-label={`${card.name}: show the card details`}
          title="Show the card details"
        >
          <CardFace
            card={card}
            sizes="220px"
            stats={creature ? { atk: Math.max(0, creature.atk), def: Math.max(0, creature.def) } : undefined}
          />
        </button>
      ) : name ? (
        <div className={styles.namedCard}><strong>{name}</strong></div>
      ) : (
        <div className={styles.emptySlot}>{label}</div>
      )}
      {label && (card || name) && <figcaption>{label}</figcaption>}
    </figure>
  );
}

function CardHistory({ lines }: { lines: { round: number; message: string }[] }) {
  const shown = lines.slice(-HISTORY_LINES);
  return (
    <div className={styles.history}>
      <p className="eyebrow">In this battle so far</p>
      {shown.length === 0 ? (
        <p>Nothing yet: the card hasn’t acted in this battle.</p>
      ) : (
        <ol>
          {lines.length > shown.length && <li className={styles.historyMore}>{lines.length - shown.length} earlier events in the log</li>}
          {shown.map((line, position) => (
            <li key={position}><span>Round {line.round}</span>{line.message}</li>
          ))}
        </ol>
      )}
    </div>
  );
}

function StatStrip({ creature }: { creature: FieldCreature }) {
  const atk = Math.max(0, creature.atk);
  const def = Math.max(0, creature.def);
  const tone = (value: number, base: number) => (value > base ? styles.up : value < base ? styles.down : "");
  return (
    <p className={styles.statStrip}>
      <span className={tone(atk, creature.baseAtk)}>ATK {atk}</span>
      <span className={tone(def, creature.baseDef)}>DEF {def}</span>
    </p>
  );
}

function Badges({ badges }: { badges: Badge[] }) {
  if (!badges.length) return null;
  return (
    <ul className={styles.badges}>
      {badges.map((badge, index) => (
        <li key={`${badge.text}-${index}`} className={badge.tone === "up" ? styles.badgeUp : badge.tone === "down" ? styles.badgeDown : ""}>{badge.text}</li>
      ))}
    </ul>
  );
}

function PlayerChip({ player, life, badges, position }: { player: Player; life: number; badges: Badge[]; position: "top" | "bottom" }) {
  return (
    <div className={`${styles.player} ${position === "top" ? styles.playerTop : styles.playerBottom}`}>
      <ProfileAvatar index={player.avatar} className={styles.avatar} />
      <span>
        <strong>{player.name}{player.you ? " · You" : ""}</strong>
        <small>PV {shownLife(life)}</small>
      </span>
      <Badges badges={badges} />
    </div>
  );
}

function SideRow({
  side,
  state,
  cards,
  badges,
  phase,
  tick,
  mirrored,
}: {
  side: Side;
  state: SideState;
  cards: CardLookup;
  badges: Badge[];
  phase: Phase;
  /** Phase index: restarts the animations without remounting the cards. */
  tick: number;
  mirrored: boolean;
}) {
  const creature = state.creature;
  const ownCards = cards[side];
  const lookup = new Map([...cards.any, ...ownCards]);
  const creatureBadges = badges.filter((badge) => badge.side === side && badge.target === "creature");
  const deck = (
    <figure className={`${styles.slot} ${styles.deck}`}>
      <span className={styles.cardBack}><Image src="/images/reversky-card-back.png" alt="" fill sizes="120px" /></span>
      <figcaption>Deck</figcaption>
    </figure>
  );
  const support = (
    <div className={styles.support}>
      {state.revealedHidden ? (
        <TableCard name={state.revealedHidden} cards={lookup} className={`${styles.small} ${styles.revealed}`} label="Hidden revealed" />
      ) : (
        <TableCard name={null} cards={lookup} className={styles.small} faceDown={state.hidden > 0} label={state.hidden > 0 ? `Hidden${state.hidden > 1 ? ` ×${state.hidden}` : ""}` : "Hidden"} />
      )}
      <TableCard name={state.item} cards={lookup} className={styles.small} label="Item" />
    </div>
  );
  const terrain = (
    <div className={styles.terrainColumn}>
      {state.spell && <TableCard name={state.spell} cards={lookup} className={`${styles.small} ${styles.spell}`} label="Spell" />}
      <TableCard name={state.terrain} cards={lookup} className={`${styles.small} ${styles.terrain}`} label="Terrain" />
    </div>
  );
  const fighter = (
    <div
      className={`${styles.fighter} ${phase.clash ? styles[`clash${mirrored ? "Down" : "Up"}${tick % 2}`] : ""} ${creature?.eliminated ? styles.eliminated : ""}`}
      key={creature?.name ?? "none"}
    >
      {creature ? (
        <>
          <TableCard name={creature.name} cards={lookup} creature={creature} className={styles.creature} />
          {creature.eliminated && <span className={styles.eliminatedTag}>Eliminated</span>}
          <StatStrip creature={creature} />
          <Badges badges={creatureBadges} key={tick} />
        </>
      ) : (
        <div className={`${styles.emptySlot} ${styles.creatureEmpty}`}>No creature</div>
      )}
    </div>
  );

  return (
    <div className={`${styles.side} ${mirrored ? styles.sideTop : styles.sideBottom}`}>
      {mirrored ? (
        <>
          <div className={`${styles.wing} ${styles.wingLeft}`}>{deck}{support}</div>
          {fighter}
          <div className={`${styles.wing} ${styles.wingRight}`}>{terrain}</div>
        </>
      ) : (
        <>
          <div className={`${styles.wing} ${styles.wingLeft} ${styles.wingEnd}`}>{terrain}</div>
          {fighter}
          <div className={`${styles.wing} ${styles.wingRight}`}>{support}{deck}</div>
        </>
      )}
    </div>
  );
}

function LogDrawer({ timeline, index, onClose }: { timeline: Timeline; index: number; onClose: () => void }) {
  const current = useRef<HTMLLIElement>(null);
  useEffect(() => {
    current.current?.scrollIntoView({ block: "center" });
  }, []);
  const shown = timeline.phases.slice(0, index + 1);
  return (
    <aside className={styles.drawer} aria-label="Battle log">
      <header>
        <h2 className="display-type">Log</h2>
        <button type="button" onClick={onClose} aria-label="Close the log">×</button>
      </header>
      <ol>
        {shown.map((phase, position) => (
          <li key={position} ref={position === index ? current : undefined} className={position === index ? styles.drawerCurrent : ""}>
            <p className="eyebrow">Round {phase.round} / {phase.label}</p>
            <ul>{phase.events.map((event, line) => <li key={line}>{event.message}</li>)}</ul>
          </li>
        ))}
      </ol>
    </aside>
  );
}

export function BattleTable({
  battle,
  timeline,
  cards,
  replay,
  players,
}: {
  battle: BattleDetail;
  timeline: Timeline;
  cards: CardLookup;
  replay: boolean;
  players: Record<Side, Player>;
}) {
  const last = timeline.phases.length - 1;
  const [index, setIndex] = useState(0);
  const [playing, setPlaying] = useState(true);
  const [logOpen, setLogOpen] = useState(false);
  const [inspected, setInspected] = useState<Inspected | null>(null);
  const resumeAfterInspect = useRef(false);
  const phase = timeline.phases[index];
  const atEnd = index >= last;

  // Auto-play: the next phase after the current one has been on the table long enough
  useEffect(() => {
    if (!playing || atEnd) return;
    const timer = window.setTimeout(() => setIndex((value) => Math.min(last, value + 1)), PHASE_MS[phase.kind]);
    return () => window.clearTimeout(timer);
  }, [playing, atEnd, index, last, phase.kind]);

  const roundPosition = useMemo(
    () => Math.max(0, timeline.roundStarts.findLastIndex((start) => start.index <= index)),
    [timeline.roundStarts, index],
  );

  const previousRound = useCallback(() => {
    const start = timeline.roundStarts[roundPosition];
    const target = index > start.index ? start.index : timeline.roundStarts[Math.max(0, roundPosition - 1)].index;
    setIndex(target);
  }, [index, roundPosition, timeline.roundStarts]);

  const nextRound = useCallback(() => {
    const next = timeline.roundStarts[roundPosition + 1];
    setIndex(next ? next.index : last);
  }, [last, roundPosition, timeline.roundStarts]);

  // Reading a card pauses the battle; closing it resumes only if it was playing
  const inspect = useCallback((value: Inspected) => {
    setPlaying((current) => {
      resumeAfterInspect.current = current;
      return false;
    });
    setInspected(value);
  }, []);
  const closeInspected = useCallback(() => {
    setInspected(null);
    if (resumeAfterInspect.current) setPlaying(true);
  }, []);

  const skipToEnd = useCallback(() => {
    setPlaying(false);
    setIndex(last);
  }, [last]);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      // The card detail handles its own keys (Escape closes it)
      if (inspected) return;
      if (event.target instanceof HTMLElement && ["INPUT", "TEXTAREA", "SELECT"].includes(event.target.tagName)) return;
      if (event.key === "ArrowRight") nextRound();
      else if (event.key === "ArrowLeft") previousRound();
      else if (event.key === " ") {
        event.preventDefault();
        setPlaying((value) => !value);
      } else if (event.key === "Escape") setLogOpen(false);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [inspected, nextRound, previousRound]);

  const board = phase.board;
  const totalRounds = timeline.roundStarts.length;
  const roundNumber = phase.round;

  return (
    <InspectContext.Provider value={inspect}>
    <div className={styles.stage}>
      <header className={styles.topBar}>
        <div className={styles.crumbs}>
          <Brand compact href="/home" />
          <span aria-hidden="true">/</span>
          <Link href="/battles">{replay ? `Replay · ${modeLabel(battle.mode)}` : modeLabel(battle.mode)}</Link>
        </div>
        <h1 className={`display-type ${styles.phaseTitle}`} aria-live="polite">
          Round {roundNumber} <span>/</span> {phase.label}
        </h1>
        <button className={styles.logButton} type="button" onClick={() => setLogOpen((value) => !value)} aria-expanded={logOpen}>Log</button>
      </header>

      <main className={styles.table}>
        <PlayerChip player={players.opponent} life={board.opponent.life} badges={phase.badges.filter((badge) => badge.side === "opponent" && badge.target === "player")} position="top" />
        <SideRow side="opponent" state={board.opponent} cards={cards} badges={phase.badges} phase={phase} tick={index} mirrored />
        <div className={`${styles.rift} ${phase.clash ? styles.riftClash : ""}`} aria-hidden="true" key={`rift-${index}`}><span>⚔</span></div>
        <SideRow side="me" state={board.me} cards={cards} badges={phase.badges} phase={phase} tick={index} mirrored={false} />
        <PlayerChip player={players.me} life={board.me.life} badges={phase.badges.filter((badge) => badge.side === "me" && badge.target === "player")} position="bottom" />

        {atEnd && (
          <div className={styles.endPanel} role="status">
            <p className="eyebrow">Match over</p>
            <h2 className="display-type">{winnerLabel(battle)}</h2>
            <Link className="button" href={`/battles/${battle.id}`}>See the result <span aria-hidden="true">›</span></Link>
          </div>
        )}
      </main>

      <footer className={styles.bottomBar}>
        <ul className={styles.caption} aria-live="polite">
          {phase.events.slice(-4).map((event, line) => <li key={`${index}-${line}`}>{event.message}</li>)}
        </ul>
        <div className={styles.progress}>
          <span>Round {roundPosition + 1} of {totalRounds}</span>
          <span className={styles.progressTrack}><span style={{ width: `${((index + 1) / timeline.phases.length) * 100}%` }} /></span>
        </div>
        <div className={styles.controls}>
          <button className="button button--quiet" type="button" onClick={previousRound} disabled={index === 0}>‹ Previous round</button>
          <button className={`button ${playing && !atEnd ? "" : "button--quiet"}`} type="button" onClick={() => setPlaying((value) => !value)} disabled={atEnd}>
            {playing && !atEnd ? "Pause" : "Play"}
          </button>
          <button className="button button--quiet" type="button" onClick={nextRound} disabled={atEnd}>Next round ›</button>
          <button className="button button--quiet" type="button" onClick={skipToEnd} disabled={atEnd}>Skip to the end »</button>
        </div>
      </footer>

      {logOpen && <LogDrawer timeline={timeline} index={index} onClose={() => setLogOpen(false)} />}

      {inspected && (
        <CardDetailDialog card={inspected.card} onClose={closeInspected}>
          {inspected.creature && (
            <p className={styles.onTable}>
              On the table now:{" "}
              <strong className={inspected.creature.atk > inspected.creature.baseAtk ? styles.up : inspected.creature.atk < inspected.creature.baseAtk ? styles.down : ""}>
                ATK {Math.max(0, inspected.creature.atk)}
              </strong>{" · "}
              <strong className={inspected.creature.def > inspected.creature.baseDef ? styles.up : inspected.creature.def < inspected.creature.baseDef ? styles.down : ""}>
                DEF {Math.max(0, inspected.creature.def)}
              </strong>
              {inspected.creature.eliminated ? " · eliminated" : ""}
            </p>
          )}
          {!inspected.creature && inspected.label && <p className={styles.onTable}>On the table as: {inspected.label}</p>}
          <CardHistory lines={cardHistory(timeline, index, inspected.card.name)} />
          <p className={styles.onTable}>The battle is paused while you read. Close to continue.</p>
        </CardDetailDialog>
      )}
    </div>
    </InspectContext.Provider>
  );
}
