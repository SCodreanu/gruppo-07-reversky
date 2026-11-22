"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
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
import styles from "./battles.module.css";

type Screen = "home" | "friendly" | "ranked" | "invites" | "history";
type InviteBox = "incoming" | "outgoing";

type BattleData = {
  decks: DeckSummary[];
  friends: FriendSummary[];
  incoming: BattleInvite[];
  outgoing: BattleInvite[];
  battles: PageResponse<BattleSummary> | null;
  ranking: PlayerRanking | null;
};

const emptyData: BattleData = {
  decks: [],
  friends: [],
  incoming: [],
  outgoing: [],
  battles: null,
  ranking: null,
};

function dateLabel(value: string): string {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function outcomeLabel(outcome: BattleSummary["outcome"]): string {
  if (outcome === "WIN") return "Victory";
  if (outcome === "LOSS") return "Defeat";
  if (outcome === "DRAW") return "Draw";
  return "Result pending";
}

function resultTone(outcome: BattleSummary["outcome"]): string {
  if (outcome === "WIN") return styles.outcomeWin;
  if (outcome === "LOSS") return styles.outcomeLoss;
  return styles.outcomeDraw;
}

function modeLabel(mode: BattleSummary["mode"]): string {
  return mode === "RANKED" ? "Ranked" : "Friendly";
}

export function BattlesExperience() {
  const router = useRouter();
  const [data, setData] = useState<BattleData>(emptyData);
  const [screen, setScreen] = useState<Screen>("home");
  const [inviteBox, setInviteBox] = useState<InviteBox>("incoming");
  const [queue, setQueue] = useState<QueueTicket | null>(null);
  const [selectedDeck, setSelectedDeck] = useState("");
  const [selectedFriend, setSelectedFriend] = useState("");
  const [expandedInvite, setExpandedInvite] = useState<string | null>(null);
  const [acceptDecks, setAcceptDecks] = useState<Record<string, string>>({});
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ kind: "success" | "error"; message: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [attempt, setAttempt] = useState(0);

  const validDecks = data.decks.filter((deck) => deck.valid);
  const invites = inviteBox === "incoming" ? data.incoming : data.outgoing;
  const queuedAt = queue?.queuedAt;
  const queuedDeckId = queue?.deckId;

  useEffect(() => {
    let active = true;

    async function load() {
      const [decks, friends, incoming, outgoing, battles, ranking, ticket] = await Promise.all([
        gameFetch<DeckSummary[]>("decks"),
        gameFetch<FriendSummary[]>("friends"),
        gameFetch<BattleInvite[]>("battle-invites?box=incoming"),
        gameFetch<BattleInvite[]>("battle-invites?box=outgoing"),
        gameFetch<PageResponse<BattleSummary>>("battles?size=10"),
        gameFetch<PlayerRanking>("rankings/me"),
        gameFetch<QueueTicket>("matchmaking/queue"),
      ]);

      if (!active) return;
      if ([decks, friends, incoming, outgoing, battles, ranking, ticket].some((result) => !result.ok && result.status === 401)) {
        router.replace(`/login?next=${encodeURIComponent("/battles")}`);
        return;
      }
      if (!decks.ok) {
        setLoadError(problemMessage(decks.problem, "We couldn’t load your decks."));
        setLoading(false);
        return;
      }

      const next: BattleData = {
        decks: decks.data,
        friends: friends.ok ? friends.data : [],
        incoming: incoming.ok ? incoming.data : [],
        outgoing: outgoing.ok ? outgoing.data : [],
        battles: battles.ok ? battles.data : null,
        ranking: ranking.ok ? ranking.data : null,
      };
      setData(next);
      setSelectedDeck((current) => current || String(next.decks.find((deck) => deck.valid)?.id ?? ""));
      setSelectedFriend((current) => current || String(next.friends[0]?.userId ?? ""));
      setQueue(ticket.ok ? ticket.data : null);
      if (ticket.ok) setScreen("ranked");
      if (!friends.ok || !incoming.ok || !outgoing.ok || !battles.ok || !ranking.ok) {
        setFeedback({ kind: "error", message: "Some battle information could not be loaded. You can still use the sections that are available." });
      }
      setLoading(false);
    }

    void load();
    return () => {
      active = false;
    };
  }, [attempt, router]);

  useEffect(() => {
    const queueTimestamp = queuedAt;
    if (!queueTimestamp || queuedDeckId === undefined) return;
    const queueTime = Date.parse(queueTimestamp);
    let active = true;
    let checking = false;
    let missedTicketChecks = 0;

    async function checkForMatch() {
      if (checking) return;
      checking = true;
      const ticket = await gameFetch<QueueTicket>("matchmaking/queue");
      if (!active) return;
      if (ticket.ok) {
        setQueue(ticket.data);
        checking = false;
        return;
      }
      if (ticket.status === 401) {
        router.replace(`/login?next=${encodeURIComponent("/battles")}`);
        checking = false;
        return;
      }
      if (ticket.status === 404) {
        missedTicketChecks += 1;
        const latest = await gameFetch<PageResponse<BattleSummary>>(
          `battles${queryString({ mode: "RANKED", size: 1 })}`,
        );
        if (!active) return;
        const battle = latest.ok
          ? latest.data.content.find((item) => Date.parse(item.createdAt) > queueTime)
          : undefined;
        if (battle) {
          router.push(`/battles/${battle.battleId}`);
          return;
        }
        if (missedTicketChecks >= 4) {
          setQueue(null);
          setFeedback({
            kind: "error",
            message: "The search ended before a match was confirmed. Check that your selected deck is still valid, then try again.",
          });
        }
        checking = false;
        return;
      }
      checking = false;
    }

    void checkForMatch();
    const interval = window.setInterval(() => void checkForMatch(), 2500);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [queuedAt, queuedDeckId, router]);

  async function refreshInvites() {
    const [incoming, outgoing] = await Promise.all([
      gameFetch<BattleInvite[]>("battle-invites?box=incoming"),
      gameFetch<BattleInvite[]>("battle-invites?box=outgoing"),
    ]);
    setData((current) => ({
      ...current,
      incoming: incoming.ok ? incoming.data : current.incoming,
      outgoing: outgoing.ok ? outgoing.data : current.outgoing,
    }));
  }

  async function refreshHistory() {
    const result = await gameFetch<PageResponse<BattleSummary>>("battles?size=10");
    if (result.ok) setData((current) => ({ ...current, battles: result.data }));
  }

  async function loadMoreHistory() {
    if (!data.battles || data.battles.page + 1 >= data.battles.totalPages || historyLoading) return;
    const page = data.battles.page + 1;
    setHistoryLoading(true);
    const result = await gameFetch<PageResponse<BattleSummary>>(`battles${queryString({ page, size: 10 })}`);
    setHistoryLoading(false);
    if (!result.ok) {
      setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t load more battles.") });
      return;
    }
    setData((current) => {
      const existing = current.battles;
      if (!existing) return { ...current, battles: result.data };
      return {
        ...current,
        battles: {
          ...result.data,
          content: [...existing.content, ...result.data.content],
        },
      };
    });
  }

  async function sendChallenge(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFriend || !selectedDeck) return;
    setPendingAction("challenge");
    setFeedback(null);
    const result = await gameFetch<BattleInvite>("battle-invites", {
      method: "POST",
      body: { opponentId: Number(selectedFriend), deckId: Number(selectedDeck) },
    });
    setPendingAction(null);
    if (!result.ok) {
      if (result.status === 401) router.replace(`/login?next=${encodeURIComponent("/battles")}`);
      else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t send the challenge.") });
      return;
    }
    setFeedback({ kind: "success", message: `Challenge sent to ${result.data.opponent.username}. You can follow it in your invitations.` });
    setInviteBox("outgoing");
    setScreen("invites");
    await refreshInvites();
  }

  async function joinRankedQueue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedDeck) return;
    setPendingAction("queue");
    setFeedback(null);
    const result = await gameFetch<QueueTicket>("matchmaking/queue", {
      method: "POST",
      body: { deckId: Number(selectedDeck) },
    });
    setPendingAction(null);
    if (!result.ok) {
      if (result.status === 401) router.replace(`/login?next=${encodeURIComponent("/battles")}`);
      else if (result.status === 409) {
        const current = await gameFetch<QueueTicket>("matchmaking/queue");
        if (current.ok) setQueue(current.data);
        else setFeedback({ kind: "error", message: problemMessage(result.problem, "You are already in the ranked queue.") });
      } else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t start the search.") });
      return;
    }
    setQueue(result.data);
    setFeedback({ kind: "success", message: "Your search has started. We’ll take you to the match as soon as the opponent is found." });
  }

  async function findBattleAfterQueue(queuedAt: string): Promise<BattleSummary | null> {
    const result = await gameFetch<PageResponse<BattleSummary>>(
      `battles${queryString({ mode: "RANKED", size: 1 })}`,
    );
    if (!result.ok) return null;
    return result.data.content.find((item) => Date.parse(item.createdAt) > Date.parse(queuedAt)) ?? null;
  }

  async function cancelQueue() {
    if (!queue) return;
    const queuedAt = queue.queuedAt;
    setPendingAction("cancel-queue");
    const result = await gameFetch<null>("matchmaking/queue", { method: "DELETE" });
    setPendingAction(null);
    if (!result.ok) {
      setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t cancel the search.") });
      return;
    }
    const matched = await findBattleAfterQueue(queuedAt);
    if (matched) {
      router.push(`/battles/${matched.battleId}`);
      return;
    }
    setQueue(null);
    setFeedback({ kind: "success", message: "Search cancelled. Your rating was not changed." });
  }

  async function acceptInvite(invite: BattleInvite) {
    const deckId = acceptDecks[invite.id] || String(validDecks[0]?.id ?? "");
    if (!deckId) return;
    setPendingAction(`accept:${invite.id}`);
    setFeedback(null);
    const result = await gameFetch<BattleDetail>(`battle-invites/${invite.id}/accept`, {
      method: "POST",
      body: { deckId: Number(deckId) },
    });
    setPendingAction(null);
    if (!result.ok) {
      if (result.status === 401) router.replace(`/login?next=${encodeURIComponent("/battles")}`);
      else if ((result.status === 503 || result.status === 500) && result.problem.battleId) {
        router.push(`/battles/${result.problem.battleId}`);
      }
      else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t accept this challenge.") });
      return;
    }
    router.push(`/battles/${result.data.id}`);
  }

  async function removeInvite(invite: BattleInvite, box: InviteBox) {
    const path = box === "incoming" ? `battle-invites/${invite.id}/reject` : `battle-invites/${invite.id}`;
    const result = await gameFetch<null>(path, { method: box === "incoming" ? "POST" : "DELETE" });
    if (!result.ok) {
      if (result.status === 401) router.replace(`/login?next=${encodeURIComponent("/battles")}`);
      else setFeedback({ kind: "error", message: problemMessage(result.problem, "We couldn’t update this invitation.") });
      return;
    }
    setData((current) => ({
      ...current,
      incoming: box === "incoming" ? current.incoming.filter((item) => item.id !== invite.id) : current.incoming,
      outgoing: box === "outgoing" ? current.outgoing.filter((item) => item.id !== invite.id) : current.outgoing,
    }));
  }

  function openHistory() {
    setScreen("history");
    setFeedback(null);
    void refreshHistory();
  }

  if (loading) {
    return <div className={styles.state} aria-live="polite"><span aria-hidden="true">✦</span><p>Opening the arena…</p></div>;
  }

  if (loadError) {
    return (
      <div className={styles.state} role="alert">
        <p>{loadError}</p>
        <button className="button button--quiet" type="button" onClick={() => {
          setLoading(true);
          setLoadError("");
          setAttempt((value) => value + 1);
        }}>Try again</button>
      </div>
    );
  }

  return (
    <div className={styles.experience}>
      {feedback && (
        <p className={`${styles.feedback} ${feedback.kind === "error" ? styles.feedbackError : styles.feedbackSuccess}`} role={feedback.kind === "error" ? "alert" : "status"}>
          {feedback.message}
        </p>
      )}

      {screen === "home" && (
        <>
          <section className={styles.hero}>
            <div className={styles.heroContent}>
              <p className="eyebrow">G02 / Your next crossing</p>
              <h2 className="display-type">Choose your battle.</h2>
              <p>Bring a valid deck. The engine will decide what waits on the other side.</p>
              <span className={styles.heroCaption}>Two theories · One table</span>
            </div>
          </section>

          <section className={styles.modeGrid} aria-label="Battle modes">
            <article className={styles.modeCard}>
              <div className={styles.cardTop}><span>01 / WITH FRIENDS</span><span aria-hidden="true">✧</span></div>
              <h3 className="display-type">Friendly challenge</h3>
              <p>Choose a friend and send an invitation. They’ll select their own deck before the battle begins.</p>
              <div className={styles.modeNote}><span>NO RATING CHANGE</span><span>SIMULATED MATCH</span></div>
              <button className="button" type="button" onClick={() => { setScreen("friendly"); setFeedback(null); }}>New challenge <span aria-hidden="true">→</span></button>
            </article>
            <article className={`${styles.modeCard} ${styles.rankedCard}`}>
              <div className={styles.cardTop}><span>02 / COMPETITIVE</span><span aria-hidden="true">⌖</span></div>
              <h3 className="display-type">Ranked match</h3>
              <p>Enter matchmaking with a valid deck. Your rating moves with every ranked result.</p>
              <div className={styles.modeNote}><span>STARTING RATING 1000</span><span>RANKED MATCHMAKING</span></div>
              <button className="button button--quiet" type="button" onClick={() => { setScreen("ranked"); setFeedback(null); }}>Find a match <span aria-hidden="true">→</span></button>
            </article>
          </section>

          <section className={styles.utilityGrid} aria-label="Battle records">
            <button className={styles.utilityCard} type="button" onClick={() => { setScreen("invites"); setFeedback(null); }}>
              <span className={styles.utilityIcon} aria-hidden="true">✉</span>
              <span className={styles.utilityText}><strong>Challenges</strong><small>{data.incoming.length} received · {data.outgoing.length} sent</small></span>
              <span className={styles.utilityArrow} aria-hidden="true">→</span>
            </button>
            <button className={styles.utilityCard} type="button" onClick={openHistory}>
              <span className={styles.utilityIcon} aria-hidden="true">◷</span>
              <span className={styles.utilityText}><strong>Battle history</strong><small>{data.battles?.totalElements ?? 0} recorded</small></span>
              <span className={styles.utilityArrow} aria-hidden="true">→</span>
            </button>
            <div className={styles.ratingCard}>
              <span className={styles.ratingLabel}>YOUR RATING</span>
              <strong>{data.ranking?.rating.toLocaleString("en-US") ?? "—"}</strong>
              <span>{data.ranking ? `${data.ranking.wins} W · ${data.ranking.losses} L · ${data.ranking.draws} D` : "Play a ranked match to begin"}</span>
            </div>
          </section>
        </>
      )}

      {screen === "friendly" && (
        <section className={styles.panel}>
          <button className={styles.backLink} type="button" onClick={() => setScreen("home")}>← Back to Play</button>
          <div className={styles.panelHeading}>
            <p className="eyebrow">G03 / Friendly</p>
            <h2 className="display-type">New challenge</h2>
            <p>Choose an opponent and one of your valid decks.</p>
          </div>
          {validDecks.length === 0 ? <NoValidDecks /> : data.friends.length === 0 ? (
            <div className={styles.emptyState}>
              <span aria-hidden="true">✧</span>
              <h3 className="display-type">No friends to challenge yet</h3>
              <p>Once a friend joins your circle, you can invite them to a friendly match here.</p>
              <Link className="text-link" href="/home">Return to your home</Link>
            </div>
          ) : (
            <form className={styles.setupForm} onSubmit={(event) => void sendChallenge(event)}>
              <label className="form-field">
                <span>Friend</span>
                <select className="text-input" value={selectedFriend} onChange={(event) => setSelectedFriend(event.target.value)} required>
                  {data.friends.map((friend) => <option key={friend.userId} value={friend.userId}>{friend.username}</option>)}
                </select>
              </label>
              <DeckSelect decks={validDecks} value={selectedDeck} onChange={setSelectedDeck} />
              <p className={styles.formHint}>Your friend will choose their own valid deck when they accept. Friendly battles do not affect rating.</p>
              <button className="button" type="submit" disabled={!selectedDeck || !selectedFriend || pendingAction === "challenge"}>
                {pendingAction === "challenge" ? "Sending invitation…" : "Send challenge"}
              </button>
            </form>
          )}
        </section>
      )}

      {screen === "ranked" && (
        <section className={styles.panel}>
          <button className={styles.backLink} type="button" onClick={() => { setScreen("home"); setFeedback(null); }}>← Back to Play</button>
          <div className={styles.panelHeading}>
            <p className="eyebrow">G05 / Ranked</p>
            <h2 className="display-type">Find an opponent</h2>
            <p>Choose a valid deck, then we’ll search for a player near your rating.</p>
          </div>
          {queue ? (
            <div className={styles.queueCard} aria-live="polite">
              <span className={styles.searchMark} aria-hidden="true">⌖</span>
              <p className="eyebrow">SEARCH IN PROGRESS</p>
              <h3 className="display-type">The arena is listening.</h3>
              <p>Searching within ±{queue.window} rating points.</p>
              <div className={styles.queueMeta}>
                <span><small>YOUR RATING</small><strong>{queue.rating.toLocaleString("en-US")}</strong></span>
                <span><small>DECK</small><strong>{data.decks.find((deck) => deck.id === queue.deckId)?.name ?? `Deck #${queue.deckId}`}</strong></span>
              </div>
              <button className="button button--quiet" type="button" onClick={() => void cancelQueue()} disabled={pendingAction === "cancel-queue"}>
                {pendingAction === "cancel-queue" ? "Cancelling…" : "Cancel search"}
              </button>
            </div>
          ) : validDecks.length === 0 ? <NoValidDecks /> : (
            <form className={styles.setupForm} onSubmit={(event) => void joinRankedQueue(event)}>
              <DeckSelect decks={validDecks} value={selectedDeck} onChange={setSelectedDeck} />
              <div className={styles.ratingPreview}>
                <span><small>CURRENT RATING</small><strong>{data.ranking?.rating.toLocaleString("en-US") ?? "1,000"}</strong></span>
                <p>Search starts close to your rating and widens as you wait.</p>
              </div>
              <button className="button" type="submit" disabled={!selectedDeck || pendingAction === "queue"}>
                {pendingAction === "queue" ? "Joining the queue…" : "Find opponent"}
              </button>
            </form>
          )}
        </section>
      )}

      {screen === "invites" && (
        <section className={styles.panel}>
          <button className={styles.backLink} type="button" onClick={() => setScreen("home")}>← Back to Play</button>
          <div className={styles.panelHeading}>
            <p className="eyebrow">G04 / Challenges</p>
            <h2 className="display-type">Battle invitations</h2>
            <p>Accept a challenge with one of your valid decks, or manage the invitations you sent.</p>
          </div>
          <div className={styles.switcher} role="tablist" aria-label="Invitation box">
            <button type="button" role="tab" aria-selected={inviteBox === "incoming"} onClick={() => { setInviteBox("incoming"); setExpandedInvite(null); }}>Received <span>{data.incoming.length}</span></button>
            <button type="button" role="tab" aria-selected={inviteBox === "outgoing"} onClick={() => { setInviteBox("outgoing"); setExpandedInvite(null); }}>Sent <span>{data.outgoing.length}</span></button>
          </div>
          {invites.length === 0 ? (
            <div className={styles.emptyState}>
              <span aria-hidden="true">✉</span>
              <h3 className="display-type">Nothing waiting at the threshold</h3>
              <p>{inviteBox === "incoming" ? "New challenges from your friends will appear here." : "Challenges you send will stay here until your friend replies."}</p>
            </div>
          ) : (
            <div className={styles.inviteList}>
              {invites.map((invite) => {
                const incoming = inviteBox === "incoming";
                const player = incoming ? invite.challenger : invite.opponent;
                const expanded = expandedInvite === invite.id;
                return (
                  <article className={styles.inviteCard} key={invite.id}>
                    <div className={styles.inviteRow}>
                      <span className={styles.inviteMark} aria-hidden="true">△</span>
                      <span className={styles.inviteCopy}>
                        <strong>{player.username} · {incoming ? "Challenge received" : "Challenge sent"}</strong>
                        <small>{incoming ? "Choose a valid deck to accept" : `Sent ${dateLabel(invite.createdAt)} · Awaiting a reply`}</small>
                      </span>
                      {incoming ? (
                        <button className={styles.inlineAction} type="button" aria-expanded={expanded} onClick={() => setExpandedInvite(expanded ? null : invite.id)}>
                          {expanded ? "Close" : "Respond"}
                        </button>
                      ) : (
                        <button className={styles.inlineAction} type="button" onClick={() => void removeInvite(invite, inviteBox)}>Withdraw</button>
                      )}
                    </div>
                    {incoming && expanded && (
                      <div className={styles.inviteActions}>
                        {validDecks.length > 0 ? (
                          <>
                            <label className="form-field">
                              <span>Your deck</span>
                              <select
                                className="text-input"
                                value={acceptDecks[invite.id] || String(validDecks[0].id)}
                                onChange={(event) => setAcceptDecks((current) => ({ ...current, [invite.id]: event.target.value }))}
                              >
                                {validDecks.map((deck) => <option key={deck.id} value={deck.id}>{deck.name} · {deck.cardCount} cards</option>)}
                              </select>
                            </label>
                            <button className="button" type="button" onClick={() => void acceptInvite(invite)} disabled={pendingAction === `accept:${invite.id}`}>
                              {pendingAction === `accept:${invite.id}` ? "Preparing battle…" : "Accept challenge"}
                            </button>
                          </>
                        ) : <NoValidDecks compact />}
                        <button className={styles.rejectButton} type="button" onClick={() => void removeInvite(invite, inviteBox)}>Decline</button>
                      </div>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </section>
      )}

      {screen === "history" && (
        <section className={styles.panel}>
          <button className={styles.backLink} type="button" onClick={() => setScreen("home")}>← Back to Play</button>
          <div className={styles.panelHeading}>
            <p className="eyebrow">G07 / Your record</p>
            <h2 className="display-type">Battle history</h2>
            <p>Every crossing leaves a trace. Open a result to review the turns.</p>
          </div>
          {!data.battles || data.battles.content.length === 0 ? (
            <div className={styles.emptyState}>
              <span aria-hidden="true">◷</span>
              <h3 className="display-type">Your first battle is still ahead</h3>
              <p>Completed battles will appear here with their result and round log.</p>
              <button className="button" type="button" onClick={() => setScreen("home")}>Choose a battle</button>
            </div>
          ) : (
            <div className={styles.historyList}>
              {data.battles.content.map((battle) => (
                <Link className={styles.historyRow} href={`/battles/${battle.battleId}`} key={battle.battleId}>
                  <span className={styles.historyMark} aria-hidden="true">{battle.mode === "RANKED" ? "⌖" : "✧"}</span>
                  <span className={styles.historyCopy}>
                    <strong>Against {battle.opponent.username}</strong>
                    <small>{modeLabel(battle.mode)} · {dateLabel(battle.createdAt)} · {battle.rounds === null ? "Result pending" : `${battle.rounds} rounds`}</small>
                  </span>
                  <span className={`${styles.outcome} ${resultTone(battle.outcome)}`}>
                    {battle.completed ? outcomeLabel(battle.outcome) : "In progress"}
                    {battle.ratingDelta !== null && <small>{battle.ratingDelta > 0 ? "+" : ""}{battle.ratingDelta} rating</small>}
                  </span>
                  <span className={styles.utilityArrow} aria-hidden="true">→</span>
                </Link>
              ))}
            </div>
          )}
          {data.battles && data.battles.page + 1 < data.battles.totalPages && (
            <button className={styles.loadMoreButton} type="button" onClick={() => void loadMoreHistory()} disabled={historyLoading}>
              {historyLoading ? "Loading battles…" : "Load more battles"}
            </button>
          )}
        </section>
      )}
    </div>
  );
}

function DeckSelect({ decks, value, onChange }: { decks: DeckSummary[]; value: string; onChange: (value: string) => void }) {
  return (
    <label className="form-field">
      <span>Valid deck</span>
      <select className="text-input" value={value} onChange={(event) => onChange(event.target.value)} required>
        {decks.map((deck) => <option key={deck.id} value={deck.id}>{deck.name} · {deck.cardCount} cards</option>)}
      </select>
    </label>
  );
}

function NoValidDecks({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`${styles.noDecks} ${compact ? styles.noDecksCompact : ""}`}>
      <span aria-hidden="true">◇</span>
      <div>
        <strong>You need a valid deck</strong>
        <p>Create one or fix an existing deck before starting a battle.</p>
      </div>
      <Link className="button button--quiet" href="/decks">Go to decks <span aria-hidden="true">→</span></Link>
    </div>
  );
}
