import type { Metadata } from "next";
import Link from "next/link";
import { PortalPageFrame } from "@/components/home/portal-page-frame";
import styles from "./page.module.css";

export const metadata: Metadata = { title: "Portal guide" };

const guideLinks = [
  { number: "01", title: "How to play", text: "Deck composition, card types, Theories, and automatic combat.", href: "/rules", label: "Read the rules" },
  { number: "02", title: "Frequently asked questions", text: "Quick answers about cards, decks, and the game portal.", href: "/faq", label: "Browse the FAQ" },
  { number: "03", title: "The world of Reversky", text: "Follow the Reverse Rain and the four competing Theories.", href: "/lore", label: "Explore the lore" },
];

export default function GuidePage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Guide"
      title="Find your way around."
      description="A quick guide to the game and the pages that help you get started."
    >
      <section className={styles.quickStart} id="first-steps">
        <div>
          <p className="eyebrow">Quick start</p>
          <h2 className="display-type">Three steps beyond the veil</h2>
        </div>
        <ol>
          <li><span>01</span><p><strong>Discover the cards.</strong> Read about the card types and the four Theories.</p></li>
          <li><span>02</span><p><strong>Plan a 30-card deck.</strong> Balance creatures with spells, hidden cards, an item, and terrain.</p></li>
          <li><span>03</span><p><strong>Watch the clash.</strong> Matches resolve automatically after a challenge is accepted.</p></li>
        </ol>
      </section>

      <section className={styles.guideGrid} aria-label="Guide pages">
        {guideLinks.map((item) => (
          <article className={styles.guideCard} key={item.number}>
            <span className={styles.number}>{item.number} / GUIDE</span>
            <h2 className="display-type">{item.title}</h2>
            <p>{item.text}</p>
            <Link href={item.href}>{item.label} <span aria-hidden="true">→</span></Link>
          </article>
        ))}
      </section>
    </PortalPageFrame>
  );
}
