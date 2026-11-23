import type { Metadata } from "next";
import { BattlesExperience } from "@/components/battles/battles-experience";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Play" };

export default function BattlesPage() {
  return (
    <PortalPageFrame eyebrow="Your portal / Play" title="Play" description="Accept a challenge, challenge a friend or look for a ranked match. Every battle is played on the table, round by round.">
      <BattlesExperience />
    </PortalPageFrame>
  );
}
