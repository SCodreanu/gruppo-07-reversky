import type { Metadata } from "next";
import Link from "next/link";
import { PublicInfoPage } from "@/components/public-info-page";
import styles from "@/components/public-info-page.module.css";

export const metadata: Metadata = {
  title: "How to play",
  description: "Learn how Reversky's 30-card decks, four Theories, and automatic battles work.",
};

const deckCounts = [
  ["20", "Creatures"],
  ["8", "Spells or Hidden cards"],
  ["1", "Item"],
  ["1", "Terrain"],
];

const cardTypes = [
  ["Creature", "Fights with ATK and DEF."],
  ["Spell", "Resolves an immediate effect."],
  ["Hidden", "Waits for a condition, then triggers its effect."],
  ["Item", "Grants a general bonus until the match ends."],
  ["Terrain", "Changes the field for the whole match."],
];

const theoryMatchups = [
  ["Invitation beats Archive", "+2 ATK for Invitation against Archive"],
  ["Archive beats Wound", "+2 ATK for Archive against Wound"],
  ["Wound beats Dream", "+2 ATK for Wound against Dream"],
  ["Dream beats Invitation", "+2 ATK for Dream against Invitation"],
];

export default function RulesPage() {
  return (
    <PublicInfoPage
      eyebrow="The game · Rules"
      title="Build the plan. Watch it unfold."
      summary="Reversky is a digital horror card game. Build a 30-card deck, challenge another player, and let the server resolve the match."
    >
      <section className={styles.section}>
        <h2>A match at a glance</h2>
        <p>
          Two players bring decks they built in advance. There is no hand to play from: after a challenge is accepted,
          the match is simulated automatically. Creatures enter the clash and card effects shape what happens next.
          The winner is the player who brings the opponent’s life points to zero.
        </p>
      </section>

      <section className={styles.section}>
        <h2>Build a 30-card deck</h2>
        <p>Every deck follows the same composition:</p>
        <div className={styles.deckGrid}>
          {deckCounts.map(([count, label]) => (
            <div className={styles.deckItem} key={label}>
              <strong>{count}</strong>
              <span>{label}</span>
            </div>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2>Know your cards</h2>
        <div className={styles.typeGrid}>
          {cardTypes.map(([name, description]) => (
            <article className={styles.typeItem} key={name}>
              <h3>{name}</h3>
              <p>{description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2>Four Theories, one cycle</h2>
        <p>
          Every creature belongs to one of the four Theories. A creature gains 2 ATK against the Theory it beats and
          loses 1 ATK against the Theory that beats it.
        </p>
        <div className={styles.theoryList}>
          {theoryMatchups.map(([matchup, bonus]) => (
            <div className={styles.theoryRow} key={matchup}>
              <strong>{matchup}</strong>
              <span className={styles.theoryBonus}>{bonus}</span>
            </div>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2>How combat resolves</h2>
        <p>Creatures clash together, and damage permanently reduces their DEF. A creature with DEF at zero or lower dies.</p>
        <div className={styles.combatGrid}>
          <article className={styles.combatCard}>
            <span>One creature falls</span>
            <p>The survivor hits the opposing player for its ATK minus the defeated creature’s remaining DEF.</p>
          </article>
          <article className={styles.combatCard}>
            <span>Both creatures fall</span>
            <p>Neither player takes damage from that clash.</p>
          </article>
          <article className={styles.combatCard}>
            <span>Both survive</span>
            <p>The clash continues into another round.</p>
          </article>
        </div>
      </section>

      <section className={styles.section}>
        <h2>Collect and trade</h2>
        <p>
          Each booster contains seven cards, with rarities ranging from Common to Mutant. Mutant cards appear at roughly
          one in forty. Trade cards with other players and revisit your challenge history at any time.
        </p>
      </section>

      <div className={styles.cta}>
        <p>Ready to build a deck and step into the Reverse Rain?</p>
        <Link className="button" href="/register">Create your account <span aria-hidden="true">→</span></Link>
      </div>
    </PublicInfoPage>
  );
}
