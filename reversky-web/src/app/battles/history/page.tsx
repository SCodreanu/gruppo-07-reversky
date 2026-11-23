import type { Metadata } from "next";
import { BattleHistory } from "@/components/battles/battle-history";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Match history" };

export default function BattleHistoryPage() {
  return (
    <PortalPageFrame eyebrow="Your portal / Play / History" title="Match history" description="Every match you played. Open the result or watch it again on the table, round by round.">
      <BattleHistory />
    </PortalPageFrame>
  );
}
