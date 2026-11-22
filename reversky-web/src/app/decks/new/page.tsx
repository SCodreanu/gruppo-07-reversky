import type { Metadata } from "next";
import { DeckBuilder } from "@/components/decks/deck-builder";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "New deck" };

export default function NewDeckPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Decks / New"
      title="Build a deck."
      description="Pick cards from your collection. The rules are checked by the server as you build."
    >
      <DeckBuilder deckId={null} />
    </PortalPageFrame>
  );
}
