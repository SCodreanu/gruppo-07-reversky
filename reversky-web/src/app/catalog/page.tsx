import type { Metadata } from "next";
import { CatalogBrowser } from "@/components/catalog/catalog-browser";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Catalog" };

export default function CatalogPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Catalog"
      title="Every card that crossed the veil."
      description="Browse the whole Reversky catalog. Filter by type, rarity or Theory and open a card to read its effect."
    >
      <CatalogBrowser />
    </PortalPageFrame>
  );
}
