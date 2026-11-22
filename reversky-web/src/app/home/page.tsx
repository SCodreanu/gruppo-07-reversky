import type { Metadata } from "next";
import { HomeDashboard } from "@/components/home/home-dashboard";

export const metadata: Metadata = { title: "Your home" };

export default async function HomePage({
  searchParams,
}: {
  searchParams: Promise<{ welcome?: string }>;
}) {
  const { welcome } = await searchParams;
  return <HomeDashboard welcome={welcome === "1"} />;
}
