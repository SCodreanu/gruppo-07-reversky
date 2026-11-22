"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { gameFetch, problemMessage } from "@/lib/api/browser";
import type { PageResponse } from "@/lib/api/contracts";

type PagedState<T> = { key: string; page?: PageResponse<T>; error?: string };

/**
 * Reads one page of `path + query` from the game BFF, debounced so typing in a filter
 * does not fire a request per key. Keeps the previous page on screen while the next one loads.
 */
export function usePaged<T>(path: string, query: string, loginNext: string) {
  const router = useRouter();
  const [state, setState] = useState<PagedState<T> | null>(null);
  const [reloads, setReloads] = useState(0);
  const key = `${path}${query}#${reloads}`;

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      void gameFetch<PageResponse<T>>(`${path}${query}`).then((result) => {
        if (!active) return;
        if (!result.ok && result.status === 401) {
          router.replace(`/login?next=${encodeURIComponent(loginNext)}`);
          return;
        }
        setState((current) => result.ok
          ? { key, page: result.data }
          : { key, page: current?.page, error: problemMessage(result.problem) });
      });
    }, 220);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [path, query, key, loginNext, router]);

  const reload = useCallback(() => setReloads((value) => value + 1), []);
  return {
    page: state?.page ?? null,
    error: state?.key === key ? state.error ?? "" : "",
    loading: state?.key !== key,
    reload,
  };
}
