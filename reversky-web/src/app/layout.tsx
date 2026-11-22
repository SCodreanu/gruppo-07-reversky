import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Reversky — The veil is lifting",
    template: "%s | Reversky",
  },
  description:
    "Enter the world of Reversky: collect cards, build your deck, and face what waits beyond the veil.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
