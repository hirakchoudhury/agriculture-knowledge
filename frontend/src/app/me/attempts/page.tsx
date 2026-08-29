"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { getMyAttempts } from "@/lib/quiz-api";
import type { AttemptSummary } from "@/lib/types";

export default function AttemptHistoryPage() {
  const { status } = useAuth();
  const router = useRouter();
  const [attempts, setAttempts] = useState<AttemptSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "loading") return;
    if (status === "anonymous") {
      router.replace("/login");
      return;
    }

    let cancelled = false;
    void (async () => {
      try {
        const page = await getMyAttempts();
        if (!cancelled) setAttempts(page.content);
      } catch (caught) {
        if (!cancelled) {
          setError(caught instanceof ApiError ? caught.message : "Could not load your attempts.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [status, router]);

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">Your quizzes</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">Attempt history</h1>

      {error && (
        <p role="alert" className="mt-6 text-sm text-danger">
          {error}
        </p>
      )}

      {loading && <p className="mt-6 text-sm text-muted">Loading…</p>}

      {!loading && attempts.length === 0 && (
        <p className="mt-6 text-sm text-muted">
          You have not finished a quiz yet.{" "}
          <Link href="/materials?type=QUIZ" className="text-accent underline underline-offset-4">
            Find one to try
          </Link>
          .
        </p>
      )}

      <ul className="mt-8 flex flex-col gap-3">
        {attempts.map((attempt) => (
          <li key={attempt.attemptId}>
            <Link
              href={`/attempts/${attempt.attemptId}`}
              className="flex flex-wrap items-baseline justify-between gap-3 rounded-md border border-line bg-surface p-4 transition-colors hover:border-accent"
            >
              <div className="min-w-0">
                <h2 className="font-medium">{attempt.quizTitle}</h2>
                <p className="font-mono text-xs text-muted">
                  {new Date(attempt.submittedAt).toLocaleString()}
                </p>
              </div>
              <div className="flex shrink-0 items-baseline gap-3">
                <span className="font-mono text-sm tabular-nums">
                  {attempt.score} / {attempt.totalMarks}
                </span>
                <span
                  className={`rounded px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider ${
                    attempt.passed ? "bg-accent-soft text-accent" : "bg-surface text-muted"
                  }`}
                >
                  {attempt.percentage}%
                </span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
