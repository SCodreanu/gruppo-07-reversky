import type { Metadata } from "next";
import { BattleResult } from "@/components/battles/battle-result";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Result" };

export default async function BattleResultPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <PortalPageFrame eyebrow="Your portal / Play / Result">
      <BattleResult battleId={id} />
    </PortalPageFrame>
  );
}
