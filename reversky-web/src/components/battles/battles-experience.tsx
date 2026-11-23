"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ProfileAvatar } from "@/components/home/profile-avatar";
import { gameFetch, problemMessage, queryString } from "@/lib/api/browser";
import type {
  BattleDetail,
  BattleInvite,
  BattleSummary,
  DeckSummary,
  FriendSummary,
  PageResponse,
  PlayerRanking,
  QueueTicket,
} from "@/lib/api/contracts";
import { playerAvatar } from "@/lib/profile-avatar";
import styles from "./play.module.css";

/** What the deck choice is for: accepting a challenge, sending one, or searching a ranked match. */
type Flow =
  | { kind: "accept"; invite: BattleInvite }
  | { kind: "challenge"; friend: FriendSummary | null }
  | { kind: "ranked" };

type HubData = {
  decks: DeckSummary[];
  friends: FriendSummary[];
  incoming: BattleInvite[];
  outgoing: BattleInvite[];
  ranking: PlayerRanking | null;
};

type Feedback = { kind: "ok" | "error"; message: string } | null;

function dateLabel(value: string): string {
  return new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function loadHub() {
  return Promise.all([
    gameFetch<DeckSummary[]>("decks"),
    gameFetch<FriendSummary[]>("friends"),
    gameFetch<BattleInvite[]>("battle-invites?box=incoming"),
    gameFetch<BattleInvite[]>("battle-invites?box=outgoing"),
    gameFetch<PlayerRanking>("rankings/me"),
    gameFetch<QueueTicket>("matchmaking/queue"),
  ]);
}

function DeckChoice({
  flow,
  decks,
  pending,
  onUse,
  onBack,
}: {
  flow: Flow;
  decks: DeckSummary[];
  pending: boolean;
  onUse: (deck: DeckSummary) => void;
  onBack: () => void;
}) {
  const context = flow.kind === "accept"
    ? `For ${flow.invite.challenger.username}’s friendly challenge`
    : flow.kind === "challenge"
      ? `Friendly challenge to ${flow.friend?.username ?? ""}`
      : "Ranked match: the opponent is chosen by rating";
  return (
    <div className={styles.column}>
      <h2 className={`display-type ${styles.sectionTitle}`}>Choose your deck</h2>
      <p className={styles.note}>{context}</p>
      {decks.length === 0 && <p className={styles.note}>You have no decks yet. Build one in the deck editor first.</p>}
      {decks.map((deck) => (
        <div className={styles.row} key={deck.id}>
          <span className={styles.rowThumb}><Image src="/images/reversky-card-back.png" alt="" fill sizes="40px" /></span>
          <span className={styles.rowText}>
            <strong>{deck.name}</strong>
            <span className={deck.valid ? "" : styles.loss}>{deck.cardCount} cards · {deck.valid ? "Valid" : "Not valid"}</span>
          </span>
          <span className={styles.rowActions}>
            {deck.valid ? (
              <button className="button" type="button" disabled={pending} onClick={() => onUse(deck)}>
                {pending ? "Please wait…" : "Use this deck"}
              </button>
            ) : (
              <Link className="button button--quiet" href={`/decks/${deck.id}`}>Fix in the editor</Link>
            )}
          </span>
        </div>
      ))}
      <div className={styles.links}>
        <Link className="button button--quiet" href="/decks">Deck editor</Link>
        <button className="button button--quiet" type="button" onClick={onBack}>Back</button>
      </div>
    </div>
  );
}

function FriendChoice({ friends, onPick, onBack }: { friends: FriendSummary[]; onPick: (friend: FriendSummary) => void; onBack: () => void }) {
  return (
    <div className={styles.column}>
      <h2 className={`display-type ${styles.sectionTitle}`}>Choose an opponent</h2>
      <p className={styles.note}>Friendly challenges can be sent to your friends.</p>
      {friends.length === 0 && <p className={styles.note}>You have no friends yet.</p>}
      {friends.map((friend) => (
        <div className={styles.row} key={friend.userId}>
          <ProfileAvatar index={playerAvatar(friend.userId)} className={styles.rowAvatar} />
          <span className={styles.rowText}><strong>{friend.username}</strong><span>Friend</span></span>
          <span className={styles.rowActions}>
            <button className="button" type="button" onClick={() => onPick(friend)}>Challenge</button>
          </span>
        </div>
      ))}
      <div className={styles.links}>
        <button className="button button--quiet" type="button" onClick={onBack}>Back</button>
      </div>
    </div>
  );
}

export function BattlesExperience() {
  const router = useRouter();
  const [data, setData] = useState<HubData | null>(null);
  const [queue, setQueue] = useState<QueueTicket | null>(null);
  const [flow, setFlow] = useState<Flow | null>(null);
  const [pending, setPending] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<Feedback>(null);
  const [loadError, setLoadError] = useState("");
  const [attempt, setAttempt] = useState(0);

  const toLogin = useCallback(() => router.replace(`/login?next=${encodeURIComponent("/battles")}`), [router]);

  useEffect(() => {
    let active = true;
    void loadHub().then(([decks, friends, incoming, outgoing, ranking, ticket]) => {
      if (!active) return;
      if ([decks, friends, incoming, outgoing, ranking, ticket].some((result) => !result.ok && result.status === 401)) {
        toLogin();
        return;
      }
      if (!decks.ok) {
        setLoadError(problemMessage(decks.problem, "We couldn’t open the arena."));
        return;
      }
      setData({
        decks: decks.data,
        friends: friends.ok ? friends.data : [],
        incoming: incoming.ok ? incoming.data : [],
        outgoing: outgoing.ok ? outgoing.data : [],
        ranking: ranking.ok ? ranking.data : null,
      });
      setQueue(ticket.ok ? ticket.data : null);
    });
    return () => {
      active = false;
    };
  }, [attempt, toLogin]);

  // Ranked search: the ticket disappears when the match is made, then the new battle is in the history
  const queuedAt = queue?.queuedAt;
  useEffect(() => {
    if (!queuedAt) return;
    const since = Date.parse(queuedAt);
    let active = true;
    let misses = 0;
    async function check() {
      const ticket = await gameFetch<QueueTicket>("matchmaking/queue");
      if (!active || ticket.ok) return;
      if (ticket.status === 401) {
        toLogin();
        return;
      }
      if (ticket.status !== 404) return;
      const latest = await gameFetch<PageResponse<BattleSummary>>(`battles${queryString({ mode: "RANKED", size: 1 })}`);
      if (!active) return;
      const battle = latest.ok ? latest.data.content.find((item) => Date.parse(item.createdAt) > since) : undefined;
      if (battle) {
        router.push(`/battles/${battle.battleId}/play`);
        return;
      }
      misses += 1;
      if (misses >= 4) {
        setQueue(null);
        setFeedback({ kind: "error", message: "The search ended without a match. Check that your deck is still valid and try again." });
      }
    }
    const timer = window.setInterval(() => void check(), 2500);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [queuedAt, router, toLogin]);

  async function refreshInvites() {
    const [incoming, outgoing] = await Promise.all([
      gameFetch<BattleInvite[]>("battle-invites?box=incoming"),
      gameFetch<BattleInvite[]>("battle-invites?box=outgoing"),
    ]);
    setData((current) => current && {
      ...current,
      incoming: incoming.ok ? incoming.data : current.incoming,
      outgoing: outgoing.ok ? outgoing.data : current.outgoing,
    });
  }

  async function chooseDeck(deck: DeckSummary) {
    if (!flow) return;
    setPending("deck");
    setFeedback(null);
    if (flow.kind === "accept") {
      const result = await gameFetch<BattleDetail>(`battle-invites/${flow.invite.id}/accept`, { method: "POST", body: { deckId: deck.id } });
      setPending(null);
      if (result.ok) router.push(`/battles/${result.data.id}/play`);
      else if (result.status === 401) toLogin();
      // Engine down: the battle exists, its result page offers the retry
      else if (result.problem.battleId) router.push(`/battles/${result.problem.battleId}`);
      else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t accept this challenge.") });
      return;
    }
    if (flow.kind === "challenge" && flow.friend) {
      const friend = flow.friend;
      const result = await gameFetch<BattleInvite>("battle-invites", { method: "POST", body: { opponentId: friend.userId, deckId: deck.id } });
      setPending(null);
      if (!result.ok) {
        if (result.status === 401) toLogin();
        else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t send the challenge.") });
        return;
      }
      setFlow(null);
      setFeedback({ kind: "ok", message: `Challenge sent to ${friend.username}. The battle starts when they accept.` });
      await refreshInvites();
      return;
    }
    if (flow.kind === "ranked") {
      const result = await gameFetch<QueueTicket>("matchmaking/queue", { method: "POST", body: { deckId: deck.id } });
      setPending(null);
      if (result.ok || result.status === 409) {
        const ticket = result.ok ? result : await gameFetch<QueueTicket>("matchmaking/queue");
        if (ticket.ok) setQueue(ticket.data);
        setFlow(null);
      } else if (result.status === 401) toLogin();
      else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t start the search.") });
    }
  }

  async function cancelQueue() {
    setPending("queue");
    const result = await gameFetch<null>("matchmaking/queue", { method: "DELETE" });
    setPending(null);
    if (!result.ok && result.status !== 404) {
      setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t cancel the search.") });
      return;
    }
    setQueue(null);
    setFeedback({ kind: "ok", message: "Search cancelled. Your rating didn’t change." });
  }

  async function removeInvite(invite: BattleInvite, incoming: boolean) {
    setPending(invite.id);
    const result = incoming
      ? await gameFetch<null>(`battle-invites/${invite.id}/reject`, { method: "POST" })
      : await gameFetch<null>(`battle-invites/${invite.id}`, { method: "DELETE" });
    setPending(null);
    if (!result.ok) {
      setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t update this challenge.") });
      return;
    }
    await refreshInvites();
  }

  if (loadError) {
    return (
      <div className={styles.column} role="alert">
        <p className={styles.note}>{loadError}</p>
        <button className="button button--quiet" type="button" onClick={() => {
          setLoadError("");
          setAttempt((value) => value + 1);
        }}>Try again</button>
      </div>
    );
  }
  if (!data) return <p className={styles.note} aria-live="polite">Opening the arena…</p>;

  const feedbackNote = feedback && (
    <p className={`${styles.feedback} ${feedback.kind === "error" ? styles.feedbackError : styles.feedbackOk}`} role={feedback.kind === "error" ? "alert" : "status"}>
      {feedback.message}
    </p>
  );

  if (flow?.kind === "challenge" && !flow.friend) {
    return <FriendChoice friends={data.friends} onPick={(friend) => setFlow({ kind: "challenge", friend })} onBack={() => setFlow(null)} />;
  }
  if (flow) {
    return (
      <>
        {feedbackNote}
        <DeckChoice flow={flow} decks={data.decks} pending={pending === "deck"} onUse={(deck) => void chooseDeck(deck)} onBack={() => setFlow(null)} />
      </>
    );
  }

  const ranking = data.ranking;
  return (
    <div className={styles.column}>
      {feedbackNote}

      {(data.incoming.length > 0 || queue) && <h2 className={`display-type ${styles.sectionTitle}`}>Waiting for you</h2>}
      {queue && (
        <div className={styles.row}>
          <span className={styles.rowText}>
            <strong>Ranked match</strong>
            <span className={styles.searching}>Searching for an opponent near {queue.rating} (±{queue.window})</span>
          </span>
          <span className={styles.rowActions}>
            <button className="button button--quiet" type="button" disabled={pending === "queue"} onClick={() => void cancelQueue()}>Cancel</button>
          </span>
        </div>
      )}
      {data.incoming.map((invite) => (
        <div className={styles.row} key={invite.id}>
          <ProfileAvatar index={playerAvatar(invite.challenger.id)} className={styles.rowAvatar} />
          <span className={styles.rowText}>
            <strong>{invite.challenger.username} vs {invite.opponent.username}</strong>
            <span>Friendly challenge · {dateLabel(invite.createdAt)}</span>
          </span>
          <span className={styles.rowActions}>
            <button className="button button--quiet" type="button" disabled={pending === invite.id} onClick={() => void removeInvite(invite, true)}>Decline</button>
            <button className="button" type="button" onClick={() => setFlow({ kind: "accept", invite })}>Enter the battle</button>
          </span>
        </div>
      ))}

      <h2 className={`display-type ${styles.sectionTitle}`}>Start a match</h2>
      <div className={styles.row}>
        <span className={styles.rowText}>
          <strong>Friendly challenge</strong>
          <span>Challenge a friend with one of your decks. No rating at stake.</span>
        </span>
        <span className={styles.rowActions}>
          <button className="button" type="button" onClick={() => setFlow({ kind: "challenge", friend: null })}>Challenge a friend</button>
        </span>
      </div>
      <div className={styles.row}>
        <span className={styles.rowText}>
          <strong>Ranked match</strong>
          <span>
            {ranking ? `Rating ${ranking.rating} · ${ranking.wins} won, ${ranking.losses} lost, ${ranking.draws} drawn. ` : ""}
            An opponent close to your rating is found for you.
          </span>
        </span>
        <span className={styles.rowActions}>
          <button className="button" type="button" disabled={Boolean(queue)} onClick={() => setFlow({ kind: "ranked" })}>
            {queue ? "Searching…" : "Find an opponent"}
          </button>
        </span>
      </div>

      {data.outgoing.length > 0 && <h2 className={`display-type ${styles.sectionTitle}`}>Waiting for an answer</h2>}
      {data.outgoing.map((invite) => (
        <div className={styles.row} key={invite.id}>
          <ProfileAvatar index={playerAvatar(invite.opponent.id)} className={styles.rowAvatar} />
          <span className={styles.rowText}>
            <strong>{invite.challenger.username} vs {invite.opponent.username}</strong>
            <span>Sent {dateLabel(invite.createdAt)}{invite.deck ? ` · ${invite.deck.name}` : ""}</span>
          </span>
          <span className={styles.rowActions}>
            <button className="button button--quiet" type="button" disabled={pending === invite.id} onClick={() => void removeInvite(invite, false)}>Withdraw</button>
          </span>
        </div>
      ))}

      <div className={styles.links}>
        <Link className="button button--quiet" href="/decks">Deck editor</Link>
        <Link className="button button--quiet" href="/battles/history">Match history</Link>
      </div>
    </div>
  );
}
