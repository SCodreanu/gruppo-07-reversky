import type { Metadata } from "next";
import { PortalPageFrame } from "@/components/home/portal-page-frame";
import { InventoryView } from "@/components/inventory/inventory-view";

export const metadata: Metadata = { title: "Inventory" };

export default function InventoryPage() {
  return (
    <PortalPageFrame
      eyebrow="Your portal / Inventory"
      title="Break the seal."
      description="The packs you bought wait here, sealed. Open one to reveal its cards and add them to your collection."
    >
      <InventoryView />
    </PortalPageFrame>
  );
}
