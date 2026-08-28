import Link from "next/link";
import { fetchPublic } from "@/lib/public-api";
import type { ExamSummary } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Exams · Agriculture Knowledge",
  description: "Syllabuses covered on Agriculture Knowledge.",
};

export default async function ExamsPage() {
  const exams = await fetchPublic<ExamSummary[]>("/api/v1/exams");

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
      <h1 className="text-3xl font-semibold tracking-tight">Exams</h1>
      <p className="mt-3 max-w-xl text-muted">
        Each exam draws on a shared pool of topics, so one topic can appear in
        several syllabuses without being duplicated.
      </p>

      {exams === null && (
        <p className="mt-8 text-sm text-danger">
          Could not reach the API. Is the backend running?
        </p>
      )}

      {exams?.length === 0 && (
        <p className="mt-8 text-sm text-muted">No exams have been published yet.</p>
      )}

      <ul className="mt-8 flex flex-col gap-3">
        {exams?.map((exam) => (
          <li key={exam.id}>
            <Link
              href={`/exams/${exam.slug}`}
              className="block rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
            >
              <div className="flex items-baseline justify-between gap-4">
                <h2 className="text-lg font-semibold">{exam.name}</h2>
                <span className="shrink-0 font-mono text-xs text-muted">
                  {exam.topicCount} {exam.topicCount === 1 ? "topic" : "topics"}
                </span>
              </div>
              {exam.description && (
                <p className="mt-2 text-sm text-muted">{exam.description}</p>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
