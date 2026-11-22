import type { Metadata } from "next";
import { BattlesExperience } from "@/components/battles/battles-experience";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Play" };

export default function BattlesPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Play"
      title="The arena awaits."
      description="Challenge a friend or enter ranked matchmaking. Every battle is simulated and recorded, turn by turn."
    >
      <BattlesExperience />
    </PortalPageFrame>
  );
}
