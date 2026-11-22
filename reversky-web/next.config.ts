import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Server Node autonomo in .next/standalone, usato dall'immagine Docker (Dockerfile).
  output: "standalone",
};

export default nextConfig;
