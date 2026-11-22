import type { Metadata } from "next";
import { DeckList } from "@/components/decks/deck-list";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Decks" };

export default function DecksPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Decks"
      title="Plan the crossing."
      description="Your saved decks. Only a deck that follows every rule can be taken into a challenge."
    >
      <DeckList />
    </PortalPageFrame>
  );
}
