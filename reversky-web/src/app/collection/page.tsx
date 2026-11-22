import type { Metadata } from "next";
import { CollectionBrowser } from "@/components/collection/collection-browser";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Collection" };

export default function CollectionPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Collection"
      title="The cards you carry."
      description="Every card you own, with your copies, the ones reserved by open trades and the decks that use them."
    >
      <CollectionBrowser />
    </PortalPageFrame>
  );
}
