import type { Metadata } from "next";
import Link from "next/link";
import { PublicInfoPage } from "@/components/public-info-page";
import styles from "@/components/public-info-page.module.css";

export const metadata: Metadata = {
  title: "The lore",
  description: "Discover the Reverse Rain, the Rifts, and the four competing Theories behind Reversky.",
};

const theories = [
  {
    name: "The Wound",
    description: "Something beyond the world has scratched it open. The Reverse Rain may be the way it bleeds.",
  },
  {
    name: "The Invitation",
    description: "Someone once asked for something to come. Now, at last, something has answered.",
  },
  {
    name: "The Archive",
    description: "The Reverse is the world’s memory. The Rifts are memories demanding to be lived again.",
  },
  {
    name: "The Dream",
    description: "Perhaps we are the Reverse, and the ordinary world is only a dream beyond the rain.",
  },
];

export default function LorePage() {
  return (
    <PublicInfoPage
      eyebrow="The world · Lore"
      title="The rain began to rise."
      summary="One ordinary afternoon, rain lifted from the ground into a cloudless sky. It lasted eleven minutes. Then the Rifts opened."
    >
      <section className={styles.section}>
        <h2>An ordinary day, turned inside out</h2>
        <div className={styles.loreCallout}>
          <p>The world did not end with a war or an explosion. The rain simply began to fall upward.</p>
        </div>
        <p className={styles.spacedParagraph}>
          For eleven minutes, the drops climbed away from the streets and disappeared into a clear sky. When the rain
          stopped, Rifts had opened in the world. Something had crossed through them, and nothing looked quite ordinary again.
        </p>
      </section>

      <section className={styles.section} id="theories">
        <h2>Four ways to explain the impossible</h2>
        <p>
          Nobody agrees on what the Reverse Rain means. The four Theories offer different explanations for the Rifts
          and the things that emerge from them. Each may be right. Each may be wrong.
        </p>
        <div className={styles.theoryCards}>
          {theories.map((theory, index) => (
            <article className={styles.theoryCard} key={theory.name}>
              <span>{String(index + 1).padStart(2, "0")} / THEORY</span>
              <h3>{theory.name}</h3>
              <p>{theory.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.section}>
        <h2>What crosses over?</h2>
        <div className={styles.loreCallout}>
          <p>The things that emerge from the Rifts are not monsters in the usual sense. They are consequences.</p>
        </div>
        <p className={styles.spacedParagraph}>
          The Reverse Rain left the world with questions instead of answers. Every card, creature, and clash offers
          another glimpse of what waits on the other side.
        </p>
      </section>

      <div className={styles.cta}>
        <p>Choose a Theory. Build a deck. Find out what waits beyond the veil.</p>
        <Link className="button" href="/register">Enter Reversky <span aria-hidden="true">→</span></Link>
      </div>
    </PublicInfoPage>
  );
}
