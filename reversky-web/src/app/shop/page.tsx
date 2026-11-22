import type { Metadata } from "next";
import { PortalPageFrame } from "@/components/home/portal-page-frame";
import { ShopExperience } from "@/components/shop/shop-experience";

export const metadata: Metadata = { title: "Shop" };

export default function ShopPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Shop"
      title="Choose a story to uncover."
      description="Spend the credits you earn in the arena on sealed Reversky packs. Every pack stays in your inventory until you are ready to reveal it."
    >
      <ShopExperience />
    </PortalPageFrame>
  );
}
