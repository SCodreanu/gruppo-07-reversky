import type { Metadata } from "next";
import { PublicInfoPage } from "@/components/public-info-page";
import styles from "@/components/public-info-page.module.css";

export const metadata: Metadata = {
  title: "FAQ",
  description: "Answers about Reversky cards, decks, matches, and the portal.",
};

const questions = [
  {
    title: "How do I get cards?",
    answer: "Cards are collected through seven-card boosters and can also be traded with other players. The collection and shop areas are still being built.",
  },
  {
    title: "Why is my deck invalid?",
    answer: "A valid deck contains exactly 30 cards: 20 Creatures, 8 Spells or Hidden cards, 1 Item, and 1 Terrain.",
  },
  {
    title: "Where can I find a match?",
    answer: "Matches are started through player challenges. The play and challenge areas are coming soon; match history will be available from the game portal once those areas are ready.",
  },
];

export default function FaqPage() {
  return (
    <PublicInfoPage
      eyebrow="Information · FAQ"
      title="Questions from beyond the veil."
      summary="A few quick answers about collecting, deck building, and how Reversky matches work."
    >
      <section className={styles.section} aria-label="Frequently asked questions">
        {questions.map((item) => (
          <details className={styles.faqItem} key={item.title}>
            <summary>{item.title}<span aria-hidden="true">+</span></summary>
            <p>{item.answer}</p>
          </details>
        ))}
      </section>
    </PublicInfoPage>
  );
}
