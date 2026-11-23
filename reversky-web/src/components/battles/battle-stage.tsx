"use client";

import Link from "next/link";
import { useState } from "react";
import { Brand } from "@/components/brand-mark";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import { playerAvatar } from "@/lib/profile-avatar";
import { BattleTable } from "./battle-table";
import { modeLabel, useBattle } from "./use-battle";
import styles from "./battle-stage.module.css";

/** Full-screen battle: intro panel on the table (live only), then the table playing the battle round by round. */
export function BattleStage({ battleId, replay }: { battleId: string; replay: boolean }) {
  const path = `/battles/${battleId}/${replay ? "replay" : "play"}`;
  const load = useBattle(battleId, path);
  const [entered, setEntered] = useState(replay);

  if (load.status !== "ready") {
    return (
      <div className={styles.shell}>
        <div className={styles.panel} role={load.status === "error" ? "alert" : "status"}>
          {load.status === "loading" ? (
            <p className={styles.loading}><span aria-hidden="true">✦</span> Preparing the table…</p>
          ) : (
            <>
              <p>{load.message}</p>
              <Link className="button button--quiet" href="/battles">Back to Play</Link>
            </>
          )}
        </div>
      </div>
    );
  }

  const { battle, timeline, cards } = load;
  const players = {
    me: { name: battle.me.username, avatar: playerAvatar(battle.me.id), you: true },
    opponent: { name: battle.opponent.username, avatar: playerAvatar(battle.opponent.id), you: false },
  };

  if (!battle.completed || !timeline.phases.length) {
    return (
      <div className={styles.shell}>
        <div className={styles.panel} role="status">
          <p className="eyebrow">{modeLabel(battle.mode)}</p>
          <h1 className="display-type">The battle hasn’t been played yet.</h1>
          <p>The simulation didn’t finish. You can retry it from the result page.</p>
          <Link className="button" href={`/battles/${battle.id}`}>Go to the result</Link>
        </div>
      </div>
    );
  }

  if (!entered) {
    return (
      <div className={styles.shell}>
        <header className={styles.topBar}>
          <Brand compact href="/home" />
          <span aria-hidden="true">/</span>
          <Link href="/battles">Play</Link>
        </header>
        <div className={styles.panel}>
          <p className={styles.mode}>{modeLabel(battle.mode)}</p>
          <h1 className="display-type">{battle.me.username} vs {battle.opponent.username}</h1>
          <div className={styles.players}>
            {[players.me, players.opponent].map((player) => (
              <div className={styles.player} key={player.name}>
                <ProfileAvatar index={player.avatar} className={styles.avatar} />
                <span>
                  <strong>{player.name}{player.you ? " · You" : ""}</strong>
                  <small>PV {timeline.startingLife}</small>
                </span>
              </div>
            ))}
          </div>
          <button className={`button ${styles.enter}`} type="button" onClick={() => setEntered(true)} autoFocus>
            Enter the battle
          </button>
          <Link className={styles.skip} href={`/battles/${battle.id}`}>Skip to the result</Link>
        </div>
      </div>
    );
  }

  return <BattleTable battle={battle} timeline={timeline} cards={cards} replay={replay} players={players} />;
}
