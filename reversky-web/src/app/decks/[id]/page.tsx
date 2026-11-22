import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { DeckBuilder } from "@/components/decks/deck-builder";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Edit deck" };

export default async function EditDeckPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  return (
    <PortalPageFrame
      eyebrow="Your portal / Decks / Edit"
      title="Refine your deck."
      description="Change the cards or the name. The rules are checked by the server as you build."
    >
      <DeckBuilder deckId={Number(id)} />
    </PortalPageFrame>
  );
}
