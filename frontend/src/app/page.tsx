import Link from "next/link";
import { fetchPublic } from "@/lib/public-api";
import type { ExamSummary } from "@/lib/types";

// The API is a separate service, so this page must render per request. Without
// this Next would try to reach the backend during `next build`, and a Vercel
// deploy would fail whenever the API happened to be down.
export const dynamic = "force-dynamic";

export default async function Home() {
  const exams = await fetchPublic<ExamSummary[]>("/api/v1/exams");

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-20">
      <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
        Agriculture Knowledge
      </h1>
      <p className="mt-4 max-w-xl text-muted">
        Articles, video lessons and practice questions for agriculture competitive
        exams. Pick a syllabus to see what it covers.
      </p>

      <section className="mt-12">
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Exams
        </h2>

        {exams === null && (
          <p className="mt-4 text-sm text-danger">
            Could not reach the API. Start the backend with{" "}
            <code className="font-mono">./mvnw spring-boot:run</code>.
          </p>
        )}

        {exams?.length === 0 && (
          <p className="mt-4 text-sm text-muted">
            Nothing published yet. An admin adds exams and topics from the admin area.
          </p>
        )}

        {exams && exams.length > 0 && (
          <ul className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {exams.map((exam) => (
              <li key={exam.id}>
                <Link
                  href={`/exams/${exam.slug}`}
                  className="flex h-full flex-col rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
                >
                  <h3 className="font-semibold">{exam.name}</h3>
                  {exam.description && (
                    <p className="mt-1.5 line-clamp-3 text-sm text-muted">
                      {exam.description}
                    </p>
                  )}
                  <span className="mt-3 font-mono text-xs text-muted">
                    {exam.topicCount} {exam.topicCount === 1 ? "topic" : "topics"}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-14 border-t border-line pt-8">
        <h2 className="font-mono text-xs uppercase tracking-[0.14em] text-muted">
          Coming next
        </h2>
        <ul className="mt-4 space-y-2 text-sm text-muted">
          <li>
            <span className="text-foreground">Phase 4 — Articles and videos.</span>{" "}
            Publishing, tagging against topics and exams, and public reading pages.
          </li>
          <li>
            <span className="text-foreground">Phase 5 — Likes and comments.</span>
          </li>
        </ul>
      </section>
    </main>
  );
}
