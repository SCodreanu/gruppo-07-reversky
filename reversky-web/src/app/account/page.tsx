import type { Metadata } from "next";
import { AccountSettings } from "@/components/home/account-settings";
import { PortalPageFrame } from "@/components/home/portal-page-frame";

export const metadata: Metadata = { title: "Manage account" };

export default function AccountPage() {
  return (
    <PortalPageFrame
      eyebrow="Profile / Manage account"
      title="Your account"
      description="Review your account details, choose a profile avatar, and manage account security."
    >
      <AccountSettings />
    </PortalPageFrame>
  );
}
