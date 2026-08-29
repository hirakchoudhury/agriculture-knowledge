"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { createPath, listPaths } from "@/lib/path-api";
import type { PathSummary } from "@/lib/types";

export default function LearningPathsPage() {
  const { status } = useAuth();
  const router = useRouter();

  const [paths, setPaths] = useState<PathSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      setPaths(await listPaths());
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not load your paths.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (status === "loading") return;
    if (status === "anonymous") {
      router.replace("/login");
      return;
    }
    void refresh();
  }, [status, router, refresh]);

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await createPath(title.trim());
      setTitle("");
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not create the path.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">Your plan</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">Learning paths</h1>
      <p className="mt-3 max-w-xl text-muted">
        Your own order through the material. Progress is remembered per item, so
        anything you finish counts everywhere it appears.
      </p>

      <form onSubmit={handleCreate} className="mt-8 flex gap-2">
        <label htmlFor="title" className="sr-only">
          New path name
        </label>
        <input
          id="title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Name a new path, e.g. Soil science revision"
          className="flex-1 rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
        />
        <button
          type="submit"
          disabled={busy || title.trim().length === 0}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          Create
        </button>
      </form>

      {error && (
        <p role="alert" className="mt-4 text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <p className="mt-8 text-sm text-muted">Loading…</p>}

      {!loading && paths.length === 0 && (
        <p className="mt-8 text-sm text-muted">
          No paths yet. Create one above, or use “Add to a path” on any article,
          video or quiz.
        </p>
      )}

      <ul className="mt-8 flex flex-col gap-3">
        {paths.map((path) => (
          <li key={path.id}>
            <Link
              href={`/me/paths/${path.id}`}
              className="block rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
            >
              <div className="flex flex-wrap items-baseline justify-between gap-3">
                <h2 className="font-semibold">{path.title}</h2>
                <span className="font-mono text-xs text-muted">
                  {path.completedCount} of {path.itemCount} done
                </span>
              </div>

              {path.description && (
                <p className="mt-1.5 text-sm text-muted">{path.description}</p>
              )}

              <ProgressBar completed={path.completedCount} total={path.itemCount} />
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}

export function ProgressBar({ completed, total }: { completed: number; total: number }) {
  const percent = total === 0 ? 0 : Math.round((completed / total) * 100);
  return (
    <div
      className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-line"
      role="progressbar"
      aria-valuenow={percent}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={`${completed} of ${total} completed`}
    >
      <div className="h-full bg-accent transition-all" style={{ width: `${percent}%` }} />
    </div>
  );
}
