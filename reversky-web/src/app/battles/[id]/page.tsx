import type { Metadata } from "next";
import { BattleResult } from "@/components/battles/battle-result";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Battle report" };

export default async function BattleResultPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <PortalPageFrame
      eyebrow="Your portal / Play / Battle report"
      title="The record remains."
      description="Review the outcome, both players’ final standing, and the sequence of events from this match."
    >
      <BattleResult battleId={id} />
    </PortalPageFrame>
  );
}
