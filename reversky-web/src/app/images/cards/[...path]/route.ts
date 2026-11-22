import { NextRequest, NextResponse } from "next/server";

const apiBase = (process.env.REVERSKY_API_URL ?? "http://localhost:8080").replace(/\/$/, "");
const SAFE_SEGMENT = /^[A-Za-z0-9._-]+$/;

/**
 * Card illustrations are public files on the API server (`imageUrl` = `/images/cards/...`).
 * This route serves them from the web origin, so the same `imageUrl` works in dev and in Docker.
 */
export async function GET(_request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  if (!path.length || !path.every((segment) => SAFE_SEGMENT.test(segment) && segment !== "." && segment !== "..")) {
    return new NextResponse(null, { status: 404 });
  }

  try {
    const upstream = await fetch(`${apiBase}/images/cards/${path.join("/")}`, { cache: "no-store" });
    if (!upstream.ok || !upstream.body) {
      // Missing art (placeholder path): short cache, so an upload shows up soon.
      return new NextResponse(null, { status: 404, headers: { "Cache-Control": "public, max-age=60" } });
    }
    return new NextResponse(upstream.body, {
      status: 200,
      headers: {
        "Content-Type": upstream.headers.get("content-type") ?? "application/octet-stream",
        // An upload gets a new file name, so a stored file never changes.
        "Cache-Control": "public, max-age=86400, immutable",
      },
    });
  } catch {
    return new NextResponse(null, { status: 503 });
  }
}
