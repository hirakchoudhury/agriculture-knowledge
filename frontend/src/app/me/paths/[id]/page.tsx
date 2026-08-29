"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ProgressBar } from "@/app/me/paths/page";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  deletePath,
  getPath,
  removeFromPath,
  reorderPath,
  setProgress,
} from "@/lib/path-api";
import type { PathDetail } from "@/lib/types";

const TYPE_LABEL: Record<string, string> = {
  ARTICLE: "Article",
  VIDEO: "Video",
  QUIZ: "Quiz",
};

export default function PathDetailPage() {
  const { id } = useParams<{ id: string }>();
  const pathId = Number(id);
  const router = useRouter();
  const { status } = useAuth();

  const [path, setPath] = useState<PathDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setPath(await getPath(pathId));
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not load that path.");
    }
  }, [pathId]);

  useEffect(() => {
    if (status === "loading") return;
    if (status === "anonymous") {
      router.replace("/login");
      return;
    }
    void refresh();
  }, [status, router, refresh]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(null);
    try {
      await action();
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  }

  /**
   * Reordering is done with move-up and move-down buttons rather than drag and
   * drop: it works with a keyboard and a screen reader, needs no library, and on
   * a phone it is easier to hit than a drag handle.
   */
  function move(index: number, direction: -1 | 1) {
    if (!path) return;
    const target = index + direction;
    if (target < 0 || target >= path.items.length) return;

    const ids = path.items.map((item) => item.itemId);
    [ids[index], ids[target]] = [ids[target], ids[index]];
    void run(() => reorderPath(pathId, ids));
  }

  if (error && !path) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
        <Link href="/me/paths" className="mt-4 inline-block text-sm text-accent underline underline-offset-4">
          Back to your paths
        </Link>
      </main>
    );
  }

  if (!path) {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Loading…</main>;
  }

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <Link href="/me/paths" className="font-mono text-xs text-muted hover:text-foreground">
        &larr; Your paths
      </Link>

      <h1 className="mt-4 text-3xl font-semibold tracking-tight">{path.title}</h1>
      {path.description && <p className="mt-2 text-muted">{path.description}</p>}

      <p className="mt-4 font-mono text-xs text-muted">
        {path.completedCount} of {path.itemCount} done
      </p>
      <ProgressBar completed={path.completedCount} total={path.itemCount} />

      {error && (
        <p role="alert" className="mt-4 text-sm text-danger">
          {error}
        </p>
      )}

      {path.items.length === 0 && (
        <p className="mt-8 text-sm text-muted">
          Nothing here yet. Open any{" "}
          <Link href="/materials" className="text-accent underline underline-offset-4">
            article, video or quiz
          </Link>{" "}
          and choose “Add to a path”.
        </p>
      )}

      <ol className="mt-8 flex flex-col gap-2">
        {path.items.map((item, index) => (
          <li
            key={item.itemId}
            className="flex flex-wrap items-center gap-3 rounded-md border border-line bg-surface p-4"
          >
            <span className="font-mono text-xs text-muted">{index + 1}</span>

            <div className="min-w-0 flex-1">
              <Link
                href={`/materials/${item.slug}`}
                className={`font-medium hover:text-accent ${
                  item.completed ? "text-muted line-through" : ""
                }`}
              >
                {item.title}
              </Link>
              <p className="font-mono text-[11px] uppercase tracking-wider text-muted">
                {TYPE_LABEL[item.type] ?? item.type}
              </p>
              {item.note && <p className="mt-1 text-sm text-muted">{item.note}</p>}
            </div>

            <div className="flex shrink-0 items-center gap-2">
              <button
                type="button"
                disabled={busy}
                onClick={() =>
                  void run(() =>
                    setProgress(item.materialId, item.completed ? "IN_PROGRESS" : "COMPLETED"),
                  )
                }
                aria-pressed={item.completed}
                className={`rounded-md border px-2 py-1 text-xs ${
                  item.completed
                    ? "border-accent bg-accent-soft text-accent"
                    : "border-line text-muted hover:border-accent"
                }`}
              >
                {item.completed ? "✓ Done" : "Mark done"}
              </button>

              <button
                type="button"
                disabled={busy || index === 0}
                onClick={() => move(index, -1)}
                aria-label={`Move ${item.title} up`}
                className="rounded border border-line px-2 py-1 text-xs text-muted disabled:opacity-40 hover:border-accent"
              >
                ↑
              </button>
              <button
                type="button"
                disabled={busy || index === path.items.length - 1}
                onClick={() => move(index, 1)}
                aria-label={`Move ${item.title} down`}
                className="rounded border border-line px-2 py-1 text-xs text-muted disabled:opacity-40 hover:border-accent"
              >
                ↓
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={() => void run(() => removeFromPath(pathId, item.itemId))}
                aria-label={`Remove ${item.title} from this path`}
                className="rounded border border-line px-2 py-1 text-xs text-danger hover:border-danger"
              >
                ✕
              </button>
            </div>
          </li>
        ))}
      </ol>

      <div className="mt-12 border-t border-line pt-6">
        <button
          type="button"
          disabled={busy}
          onClick={() =>
            void run(async () => {
              await deletePath(pathId);
              router.replace("/me/paths");
            })
          }
          className="text-sm text-danger underline underline-offset-4"
        >
          Delete this path
        </button>
        <p className="mt-2 text-xs text-muted">
          Deleting a path removes the plan, not the material, and keeps everything
          you have marked as done.
        </p>
      </div>
    </main>
  );
}
