import type { BattleDetail, BattleEvent } from "@/lib/api/contracts";

/**
 * Turns the battle log of GET /battles/{id} into rounds made of phases, as the table plays them:
 * deployment (creatures entering, spells, Hidden, Item, Terrain), effects before the attack, battle
 * (both attacks at once), effects after the battle, points (eliminations and life points lost).
 * The engine logs the replacement creature at the end of a round; here it opens the next round,
 * as in the mockup ("Round 2 / Reinforcements").
 */

export type Side = "me" | "opponent";
export type PhaseKind = "deploy" | "effects" | "battle" | "after" | "points" | "end";

export type FieldCreature = {
  name: string;
  atk: number;
  def: number;
  baseAtk: number;
  baseDef: number;
  eliminated: boolean;
};

export type SideState = {
  life: number;
  creature: FieldCreature | null;
  terrain: string | null;
  item: string | null;
  /** Hidden cards set face down. */
  hidden: number;
  /** Shown only during the phase where it happens. */
  revealedHidden: string | null;
  spell: string | null;
};

export type Board = Record<Side, SideState>;

export type Badge = { side: Side; target: "creature" | "player"; text: string; tone: "down" | "up" | "neutral" };

export type Phase = {
  round: number;
  kind: PhaseKind;
  label: string;
  events: BattleEvent[];
  board: Board;
  badges: Badge[];
  /** Both creatures clash in this phase. */
  clash: boolean;
};

export type Timeline = {
  phases: Phase[];
  /** Index of the first phase of each round, in round order. */
  roundStarts: { round: number; index: number }[];
  startingLife: number;
};

const DEPLOY_TYPES = new Set(["CREATURE_ENTERED", "CARD_DRAWN", "SPELL_RESOLVED", "HIDDEN_SET", "TERRAIN_PLACED", "ITEM_PLACED"]);
const EFFECT_TYPES = new Set(["EFFECT_TRIGGERED", "HIDDEN_TRIGGERED", "ITEM_CONSUMED"]);
const POINT_TYPES = new Set(["CREATURE_DIED", "PLAYER_DAMAGED"]);

function num(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function str(value: unknown): string | null {
  return typeof value === "string" && value.length > 0 ? value : null;
}

function emptySide(life: number): SideState {
  return { life, creature: null, terrain: null, item: null, hidden: 0, revealedHidden: null, spell: null };
}

function cloneBoard(board: Board): Board {
  const copy = (side: SideState): SideState => ({ ...side, creature: side.creature ? { ...side.creature } : null });
  return { me: copy(board.me), opponent: copy(board.opponent) };
}

function other(side: Side): Side {
  return side === "me" ? "opponent" : "me";
}

/** "+2 ATK, −5 DEF from …" -> "+2 ATK, −5 DEF" (the change, for a badge). */
function changeText(description: string): string | null {
  const match = description.match(/^[^:]*?([+−-]\d+ (?:ATK|DEF)(?:, [+−-]\d+ (?:ATK|DEF))?)/);
  return match ? match[1].replace("-", "−") : null;
}

/** Applies the values that an effect description ends with, e.g. "(ATK 6, DEF 4)", "(ATK 8)", "(DEF 13)". */
function applyStatsFromText(creature: FieldCreature, text: string): void {
  const both = text.match(/\(ATK (-?\d+), DEF (-?\d+)\)\s*(?:,|$)/);
  const atkOnly = text.match(/\(ATK (-?\d+)\)\s*(?:,|$)/);
  const defOnly = text.match(/\(DEF (-?\d+)\)\s*(?:,|$)/);
  if (both) {
    creature.atk = Number(both[1]);
    creature.def = Number(both[2]);
  } else if (atkOnly) {
    creature.atk = Number(atkOnly[1]);
  } else if (defOnly) {
    creature.def = Number(defOnly[1]);
  }
}

function phaseLabel(kind: PhaseKind, round: number): string {
  switch (kind) {
    case "deploy":
      return round <= 1 ? "Deployment" : "Reinforcements";
    case "effects":
      return "Effects";
    case "battle":
      return "Battle";
    case "after":
      return "After-battle effects";
    case "points":
      return "Points";
    case "end":
      return "Match over";
  }
}

/** The engine writes "Challenger" and "Opponent": the table shows the players' names. */
function withNames(battle: BattleDetail): BattleEvent[] {
  const challenger = battle.mySide === "CHALLENGER" ? battle.me.username : battle.opponent.username;
  const opponent = battle.mySide === "CHALLENGER" ? battle.opponent.username : battle.me.username;
  return battle.log.map((event) => ({
    ...event,
    message: event.message?.replace(/\bChallenger\b/g, challenger).replace(/\bOpponent\b/g, opponent),
  }));
}

/** Life points as shown on the table: never below 0, as in the mockup. */
export function shownLife(value: number): number {
  return Math.max(0, value);
}

export function buildTimeline(battle: BattleDetail): Timeline {
  const log = withNames(battle);
  const sideOf = (event: BattleEvent): Side | null =>
    event.side ? (event.side === battle.mySide ? "me" : "opponent") : null;
  const start = log.find((event) => event.type === "MATCH_STARTED");
  const startingLife = num(start?.data?.startingLifePoints) ?? 30;

  // 1. Buckets of events per round and phase
  const byRound = new Map<number, BattleEvent[]>();
  for (const event of log) {
    if (event.type === "MATCH_STARTED" || event.type === "ROUND_STARTED") continue;
    const round = event.round ?? 0;
    byRound.set(round, [...(byRound.get(round) ?? []), event]);
  }
  const rounds = [...byRound.keys()].filter((round) => round > 0).sort((a, b) => a - b);
  const buckets: { round: number; kind: PhaseKind; events: BattleEvent[] }[] = [];
  let carried: BattleEvent[] = byRound.get(0) ?? [];
  if (!rounds.length && carried.length) buckets.push({ round: 1, kind: "deploy", events: carried });

  rounds.forEach((round, position) => {
    const events = byRound.get(round) ?? [];
    const firstAttack = events.findIndex((event) => event.type === "ATTACK_COMPUTED");
    let lastDamage = -1;
    events.forEach((event, index) => {
      if (event.type === "DAMAGE_DEALT") lastDamage = index;
    });
    const battleStart = firstAttack >= 0 ? firstAttack : events.length;
    const battleEnd = lastDamage >= battleStart ? lastDamage : battleStart - 1;
    const before = events.slice(0, battleStart);
    const fight = events.slice(battleStart, battleEnd + 1);
    const after: BattleEvent[] = [];
    const points: BattleEvent[] = [];
    const next: BattleEvent[] = [];
    const end: BattleEvent[] = [];
    let deploying = false;
    for (const event of events.slice(battleEnd + 1)) {
      const type = event.type ?? "";
      if (type === "MATCH_ENDED") end.push(event);
      else if (POINT_TYPES.has(type)) points.push(event);
      else if (DEPLOY_TYPES.has(type) || (deploying && EFFECT_TYPES.has(type))) {
        deploying = true;
        next.push(event);
      } else if (points.length === 0 && EFFECT_TYPES.has(type)) after.push(event);
      else next.push(event);
    }

    const lastRound = position === rounds.length - 1;
    if (carried.length) buckets.push({ round, kind: "deploy", events: carried });
    if (before.length) buckets.push({ round, kind: "effects", events: before });
    if (fight.length) buckets.push({ round, kind: "battle", events: fight });
    if (after.length) buckets.push({ round, kind: "after", events: after });
    if (points.length) buckets.push({ round, kind: "points", events: points });
    if (lastRound && next.length) buckets.push({ round, kind: "deploy", events: next });
    if (end.length) buckets.push({ round, kind: "end", events: end });
    carried = lastRound ? [] : next;
  });

  // 2. Board after each phase
  const board: Board = { me: emptySide(startingLife), opponent: emptySide(startingLife) };
  const phases: Phase[] = [];
  for (const bucket of buckets) {
    board.me.revealedHidden = null;
    board.opponent.revealedHidden = null;
    board.me.spell = null;
    board.opponent.spell = null;
    const badges: Badge[] = [];
    let clash = false;

    for (const event of bucket.events) {
      const side = sideOf(event);
      const data = event.data ?? {};
      const card = str(data.card);
      switch (event.type) {
        case "CREATURE_ENTERED": {
          const atk = num(data.attack) ?? 0;
          const def = num(data.defense) ?? 0;
          if (side && card) board[side].creature = { name: card, atk, def, baseAtk: atk, baseDef: def, eliminated: false };
          break;
        }
        case "SPELL_RESOLVED":
          if (side && card) board[side].spell = card;
          break;
        case "HIDDEN_SET":
          if (side) board[side].hidden += 1;
          break;
        case "HIDDEN_TRIGGERED":
          if (side) {
            board[side].hidden = Math.max(0, board[side].hidden - 1);
            board[side].revealedHidden = card;
            const effect = str(data.effect);
            if (effect) badges.push({ side, target: "creature", text: effect.replace(/ to .*$/, ""), tone: "up" });
          }
          break;
        case "TERRAIN_PLACED": {
          const replaced = str(data.replaced);
          if (replaced) {
            if (board.me.terrain === replaced) board.me.terrain = null;
            if (board.opponent.terrain === replaced) board.opponent.terrain = null;
          }
          if (side && card) board[side].terrain = card;
          break;
        }
        case "ITEM_PLACED":
          if (side && card) board[side].item = card;
          break;
        case "ITEM_CONSUMED":
          if (side && board[side].item === card) board[side].item = null;
          break;
        case "EFFECT_TRIGGERED": {
          const description = str(data.description) ?? "";
          const damage = description.match(/deals (\d+) damage to (.+?) \(DEF (-?\d+)\)/);
          // The values belong to the creature named in the text when it is the other one (e.g. "deals 2 damage to X")
          let target: Side | null = side;
          if (damage) target = board.me.creature?.name === damage[2] ? "me" : board.opponent.creature?.name === damage[2] ? "opponent" : null;
          else if (side && board[other(side)].creature && description.includes(board[other(side)].creature!.name)) target = other(side);
          else if (side && board[side].creature?.name !== card && board[other(side)].creature?.name === card) target = other(side);
          const creature = target ? board[target].creature : null;
          if (creature && target) {
            if (damage) {
              creature.def = Number(damage[3]);
              badges.push({ side: target, target: "creature", text: `−${damage[1]} DEF`, tone: "down" });
            } else {
              applyStatsFromText(creature, description);
              const change = changeText(description);
              if (change) badges.push({ side: target, target: "creature", text: change, tone: change.includes("−") ? "down" : "up" });
            }
          }
          break;
        }
        case "ATTACK_COMPUTED": {
          clash = true;
          const total = num(data.total);
          if (side && total !== null && board[side].creature) board[side].creature!.atk = total;
          break;
        }
        case "DAMAGE_DEALT": {
          clash = true;
          const target = side ? other(side) : null;
          const amount = num(data.amount);
          const defense = num(data.targetDefense);
          if (target && board[target].creature) {
            if (defense !== null) board[target].creature!.def = defense;
            if (amount !== null) badges.push({ side: target, target: "creature", text: `−${amount} DEF`, tone: "down" });
          }
          break;
        }
        case "CREATURE_DIED":
          if (side && board[side].creature) board[side].creature!.eliminated = true;
          break;
        case "PLAYER_DAMAGED": {
          const life = num(data.lifePoints);
          const amount = num(data.amount);
          if (side) {
            if (life !== null) board[side].life = life;
            if (amount !== null) badges.push({ side, target: "player", text: `−${amount} PV`, tone: "down" });
          }
          break;
        }
        default:
          break;
      }
    }

    phases.push({
      round: bucket.round,
      kind: bucket.kind,
      label: phaseLabel(bucket.kind, bucket.round),
      events: bucket.events,
      board: cloneBoard(board),
      badges,
      clash: clash && bucket.kind === "battle",
    });
  }

  const roundStarts: Timeline["roundStarts"] = [];
  phases.forEach((phase, index) => {
    if (!roundStarts.length || roundStarts[roundStarts.length - 1].round !== phase.round) roundStarts.push({ round: phase.round, index });
  });
  return { phases, roundStarts, startingLife };
}

/** The final life points from the API win over the replay (they include every effect). */
export function finalLife(battle: BattleDetail, timeline: Timeline): Record<Side, number> {
  const last = timeline.phases[timeline.phases.length - 1]?.board;
  return {
    me: battle.me.finalLifePoints ?? last?.me.life ?? timeline.startingLife,
    opponent: battle.opponent.finalLifePoints ?? last?.opponent.life ?? timeline.startingLife,
  };
}
