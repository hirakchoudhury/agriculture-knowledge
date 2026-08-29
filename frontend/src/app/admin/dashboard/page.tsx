"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { getAdminStats } from "@/lib/admin-api";
import type { AdminStats } from "@/lib/types";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        setStats(await getAdminStats());
      } catch (caught) {
        setError(caught instanceof ApiError ? caught.message : "Could not load the numbers.");
      }
    })();
  }, []);

  if (error) {
    return (
      <p role="alert" className="text-sm text-danger">
        {error}
      </p>
    );
  }

  if (!stats) {
    return <p className="text-sm text-muted">Loading…</p>;
  }

  const published = stats.materialsByStatus.PUBLISHED ?? 0;
  const drafts = stats.materialsByStatus.DRAFT ?? 0;

  return (
    <div className="flex flex-col gap-10">
      <section>
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Published material
        </h2>
        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Articles" value={stats.materialsByType.ARTICLE ?? 0} />
          <Stat label="Videos" value={stats.materialsByType.VIDEO ?? 0} />
          <Stat label="Quizzes" value={stats.materialsByType.QUIZ ?? 0} />
          <Stat
            label="Drafts"
            value={drafts}
            hint={drafts > 0 ? "waiting to publish" : undefined}
          />
        </div>
      </section>

      <section>
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Catalogue
        </h2>
        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Exams" value={stats.exams} />
          <Stat label="Topics" value={stats.topics} />
          <Stat label="Archived" value={stats.materialsByStatus.ARCHIVED ?? 0} />
          <Stat label="Total published" value={published} />
        </div>
      </section>

      <section>
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          People and activity
        </h2>
        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Accounts" value={stats.users} hint={`${stats.admins} admin`} />
          <Stat label="Comments" value={stats.comments} />
          <Stat label="Likes" value={stats.likes} />
          <Stat label="Quiz attempts" value={stats.quizAttempts} />
        </div>
        <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Stat label="Learning paths" value={stats.learningPaths} />
        </div>
      </section>

      <section>
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Most read
        </h2>

        {stats.mostViewed.length === 0 ? (
          <p className="mt-4 text-sm text-muted">
            Nothing has been read yet. Publish something and it will show up here.
          </p>
        ) : (
          <ul className="mt-4 flex flex-col gap-2">
            {stats.mostViewed.map((material) => (
              <li key={material.id}>
                <Link
                  href={`/materials/${material.slug}`}
                  className="flex flex-wrap items-baseline justify-between gap-3 rounded-md border border-line bg-surface px-4 py-3 transition-colors hover:border-accent"
                >
                  <span className="min-w-0">
                    <span className="font-mono text-[10px] uppercase tracking-wider text-muted">
                      {material.type}
                    </span>
                    <span className="ml-2 text-sm font-medium">{material.title}</span>
                  </span>
                  <span className="shrink-0 font-mono text-xs tabular-nums text-muted">
                    {material.viewCount} views · {material.likeCount} likes
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function Stat({ label, value, hint }: { label: string; value: number; hint?: string }) {
  return (
    <div className="rounded-md border border-line bg-surface p-4">
      <p className="font-mono text-[10px] uppercase tracking-[0.12em] text-muted">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      {hint && <p className="mt-0.5 text-xs text-muted">{hint}</p>}
    </div>
  );
}
