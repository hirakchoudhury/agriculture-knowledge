"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { addToPath, createPath, getProgress, listPaths, setProgress } from "@/lib/path-api";
import type { PathSummary } from "@/lib/types";

/**
 * Mark-as-done and add-to-path, both of which need to know who is reading.
 *
 * The page is server-rendered without the viewer's token, so this waits for the
 * auth context before asking the server anything.
 */
export function MaterialActions({ materialId }: { materialId: number }) {
  const { status } = useAuth();

  const [completed, setCompleted] = useState(false);
  const [paths, setPaths] = useState<PathSummary[]>([]);
  const [picking, setPicking] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status !== "authenticated") return;
    let cancelled = false;
    void (async () => {
      try {
        const [progress, myPaths] = await Promise.all([getProgress(materialId), listPaths()]);
        if (cancelled) return;
        setCompleted(progress.completed);
        setPaths(myPaths);
      } catch {
        // Non-fatal: the reader can still read.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [materialId, status]);

  if (status !== "authenticated") {
    return null;
  }

  async function toggleComplete() {
    setBusy(true);
    setError(null);
    const next = !completed;
    setCompleted(next);
    try {
      await setProgress(materialId, next ? "COMPLETED" : "IN_PROGRESS");
      setNotice(next ? "Marked as done." : "Marked as not done.");
    } catch (caught) {
      setCompleted(!next);
      setError(caught instanceof ApiError ? caught.message : "Could not save that.");
    } finally {
      setBusy(false);
    }
  }

  async function add(pathId: number) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await addToPath(pathId, materialId);
      setPicking(false);
      setNotice(`Added to "${updated.title}".`);
      setPaths((current) =>
        current.map((path) =>
          path.id === pathId ? { ...path, itemCount: updated.itemCount } : path,
        ),
      );
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not add it.");
    } finally {
      setBusy(false);
    }
  }

  async function addToNew(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const created = await createPath(newTitle.trim());
      const updated = await addToPath(created.id, materialId);
      setPaths((current) => [
        {
          id: updated.id,
          title: updated.title,
          description: updated.description,
          itemCount: updated.itemCount,
          completedCount: updated.completedCount,
          createdAt: updated.createdAt,
        },
        ...current,
      ]);
      setNewTitle("");
      setPicking(false);
      setNotice(`Created "${updated.title}" and added this to it.`);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not create the path.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mt-4 flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={toggleComplete}
          disabled={busy}
          aria-pressed={completed}
          className={`rounded-md border px-3 py-1.5 text-sm transition-colors ${
            completed
              ? "border-accent bg-accent-soft text-accent"
              : "border-line text-muted hover:border-accent hover:text-foreground"
          }`}
        >
          {completed ? "✓ Done" : "Mark as done"}
        </button>

        <button
          type="button"
          onClick={() => setPicking((open) => !open)}
          className="rounded-md border border-line px-3 py-1.5 text-sm text-muted hover:border-accent hover:text-foreground"
        >
          {picking ? "Cancel" : "Add to a path"}
        </button>
      </div>

      {notice && <p className="text-xs text-accent">{notice}</p>}
      {error && (
        <p role="alert" className="text-xs text-danger">
          {error}
        </p>
      )}

      {picking && (
        <div className="rounded-md border border-line bg-surface p-4">
          {paths.length > 0 && (
            <ul className="flex flex-col gap-1">
              {paths.map((path) => (
                <li key={path.id}>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => void add(path.id)}
                    className="w-full rounded px-2 py-1.5 text-left text-sm hover:bg-accent-soft"
                  >
                    {path.title}
                    <span className="ml-2 font-mono text-xs text-muted">
                      {path.itemCount} step{path.itemCount === 1 ? "" : "s"}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <form
            onSubmit={addToNew}
            className={`flex gap-2 ${paths.length > 0 ? "mt-3 border-t border-line pt-3" : ""}`}
          >
            <label htmlFor="new-path" className="sr-only">
              New path name
            </label>
            <input
              id="new-path"
              value={newTitle}
              onChange={(event) => setNewTitle(event.target.value)}
              placeholder="Or start a new path…"
              className="flex-1 rounded-md border border-line bg-background px-3 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
            />
            <button
              type="submit"
              disabled={busy || newTitle.trim().length === 0}
              className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-background disabled:opacity-60"
            >
              Create
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
