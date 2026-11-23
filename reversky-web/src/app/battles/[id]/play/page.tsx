import type { Metadata } from "next";
import { BattleStage } from "@/components/battles/battle-stage";

export const metadata: Metadata = { title: "Battle" };

export default async function PlayBattlePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <BattleStage battleId={id} replay={false} />;
}
