"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { archiveMaterial, listAdminMaterials, setMaterialStatus } from "@/lib/material-api";
import type { MaterialStatus, MaterialSummary, MaterialType } from "@/lib/types";

const STATUS_STYLE: Record<MaterialStatus, string> = {
  DRAFT: "bg-warn/15 text-warn",
  PUBLISHED: "bg-accent-soft text-accent",
  ARCHIVED: "bg-surface text-muted",
};

export default function AdminMaterialsPage() {
  const [items, setItems] = useState<MaterialSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [status, setStatus] = useState<MaterialStatus | "">("");
  const [type, setType] = useState<MaterialType | "">("");

  const refresh = useCallback(async () => {
    try {
      const page = await listAdminMaterials({ status, type });
      setItems(page.content);
      setTotal(page.totalElements);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not load material.");
    } finally {
      setLoading(false);
    }
  }, [status, type]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

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

  const selectClass =
    "rounded-md border border-line bg-surface px-3 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">
          Material {!loading && <span className="text-muted">({total})</span>}
        </h2>
        <div className="flex gap-2">
          <Link
            href="/admin/materials/new/article"
            className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-background"
          >
            New article
          </Link>
          <Link
            href="/admin/materials/new/video"
            className="rounded-md border border-line px-3 py-1.5 text-sm font-medium hover:border-accent"
          >
            New video
          </Link>
          <Link
            href="/admin/quizzes/new"
            className="rounded-md border border-line px-3 py-1.5 text-sm font-medium hover:border-accent"
          >
            New quiz
          </Link>
        </div>
      </section>

      <div className="flex flex-wrap gap-2">
        <select
          aria-label="Status"
          value={status}
          onChange={(event) => setStatus(event.target.value as MaterialStatus | "")}
          className={selectClass}
        >
          <option value="">Any status</option>
          <option value="DRAFT">Drafts</option>
          <option value="PUBLISHED">Published</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        <select
          aria-label="Format"
          value={type}
          onChange={(event) => setType(event.target.value as MaterialType | "")}
          className={selectClass}
        >
          <option value="">Any format</option>
          <option value="ARTICLE">Articles</option>
          <option value="VIDEO">Videos</option>
          <option value="QUIZ">Quizzes</option>
        </select>
      </div>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <p className="text-sm text-muted">Loading…</p>}
      {!loading && items.length === 0 && (
        <p className="text-sm text-muted">Nothing here yet.</p>
      )}

      <ul className="flex flex-col gap-3">
        {items.map((material) => (
          <li key={material.id} className="rounded-md border border-line bg-surface p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span
                    className={`rounded px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${STATUS_STYLE[material.status]}`}
                  >
                    {material.status}
                  </span>
                  <span className="font-mono text-[11px] uppercase tracking-wider text-muted">
                    {material.type}
                  </span>
                </div>
                <h3 className="mt-1.5 font-medium">{material.title}</h3>
                <p className="font-mono text-xs text-muted">/materials/{material.slug}</p>
              </div>

              <div className="flex shrink-0 flex-wrap items-center gap-3 text-sm">
                {material.type === "QUIZ" && (
                  <Link
                    href={`/admin/quizzes/${material.id}`}
                    className="text-accent underline underline-offset-4"
                  >
                    Questions
                  </Link>
                )}

                {material.status === "PUBLISHED" ? (
                  <Link
                    href={`/materials/${material.slug}`}
                    className="text-muted underline underline-offset-4 hover:text-foreground"
                  >
                    View
                  </Link>
                ) : (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => void run(() => setMaterialStatus(material.id, "PUBLISHED"))}
                    className="text-accent underline underline-offset-4"
                  >
                    Publish
                  </button>
                )}

                {material.status === "PUBLISHED" && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => void run(() => setMaterialStatus(material.id, "DRAFT"))}
                    className="text-muted underline underline-offset-4 hover:text-foreground"
                  >
                    Unpublish
                  </button>
                )}

                {material.status !== "ARCHIVED" && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => void run(() => archiveMaterial(material.id))}
                    className="text-danger underline underline-offset-4"
                  >
                    Archive
                  </button>
                )}
              </div>
            </div>
          </li>
        ))}
      </ul>

      <p className="text-xs text-muted">
        Archiving hides material from the site without deleting it — comments and
        progress from later phases point at these rows.
      </p>
    </div>
  );
}
