"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { getLikeStatus, like, unlike } from "@/lib/engagement-api";

/**
 * Optimistic: the count moves on click and only reverts if the server disagrees.
 * Waiting for a round trip to acknowledge a like makes the whole page feel slow,
 * and the failure case is rare and harmless.
 */
export function LikeButton({
  materialId,
  initialCount,
}: {
  materialId: number;
  initialCount: number;
}) {
  const { status } = useAuth();
  const [liked, setLiked] = useState(false);
  const [count, setCount] = useState(initialCount);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // The page is server-rendered without the viewer's token, so the initial HTML
  // always says "not liked". Correct that once the session is known.
  useEffect(() => {
    if (status !== "authenticated") {
      setLiked(false);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const current = await getLikeStatus(materialId);
        if (!cancelled) {
          setLiked(current.liked);
          setCount(current.likeCount);
        }
      } catch {
        // Leave the server-rendered count in place; the button still works.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [materialId, status]);

  async function toggle() {
    if (status !== "authenticated" || busy) {
      return;
    }

    const previousLiked = liked;
    const previousCount = count;

    setLiked(!previousLiked);
    setCount(previousCount + (previousLiked ? -1 : 1));
    setBusy(true);
    setError(null);

    try {
      const result = previousLiked ? await unlike(materialId) : await like(materialId);
      // Settle on the server's number: other people may have liked it meanwhile.
      setLiked(result.liked);
      setCount(result.likeCount);
    } catch (caught) {
      setLiked(previousLiked);
      setCount(previousCount);
      setError(
        caught instanceof ApiError ? caught.message : "Could not save that just now.",
      );
    } finally {
      setBusy(false);
    }
  }

  const signedIn = status === "authenticated";

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={toggle}
        disabled={!signedIn || busy}
        aria-pressed={liked}
        title={signedIn ? undefined : "Sign in to like this"}
        className={`flex items-center gap-2 rounded-md border px-3 py-1.5 text-sm transition-colors ${
          liked
            ? "border-accent bg-accent-soft text-accent"
            : "border-line text-muted hover:border-accent hover:text-foreground"
        } ${signedIn ? "" : "cursor-not-allowed opacity-70"}`}
      >
        <span aria-hidden>{liked ? "♥" : "♡"}</span>
        <span className="font-mono text-xs tabular-nums">{count}</span>
        <span className="sr-only">
          {liked ? "Unlike this material" : "Like this material"}
        </span>
      </button>

      {error && (
        <span role="alert" className="text-xs text-danger">
          {error}
        </span>
      )}
    </div>
  );
}
