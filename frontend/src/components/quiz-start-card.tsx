import Link from "next/link";
import { getQuizSummary } from "@/lib/quiz-api";

/**
 * Shown in place of a body when the material is a quiz. Server-rendered, so the
 * question count and marks are visible to search engines and signed-out visitors.
 */
export async function QuizStartCard({ slug }: { slug: string }) {
  const quiz = await getQuizSummary(slug);

  if (!quiz) {
    return (
      <p className="rounded-md border border-line bg-surface p-5 text-sm text-muted">
        This quiz is not available right now.
      </p>
    );
  }

  const minutes = quiz.timeLimitSeconds ? Math.round(quiz.timeLimitSeconds / 60) : null;

  return (
    <section className="rounded-md border border-line bg-surface p-6">
      <dl className="grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-4">
        <Stat label="Questions" value={String(quiz.questionCount)} />
        <Stat label="Marks" value={String(quiz.totalMarks)} />
        <Stat label="Time" value={minutes ? `${minutes} min` : "Untimed"} />
        <Stat label="Pass mark" value={`${quiz.passPercentage}%`} />
      </dl>

      <Link
        href={`/quizzes/${slug}/attempt`}
        className="mt-6 inline-block rounded-md bg-accent px-5 py-2 text-sm font-medium text-background"
      >
        {quiz.attemptsByMe > 0 ? "Attempt again" : "Start quiz"}
      </Link>

      <p className="mt-3 text-xs text-muted">
        Marked as soon as you submit, with an explanation for every question.
      </p>
    </section>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-mono text-[10px] uppercase tracking-[0.12em] text-muted">{label}</dt>
      <dd className="mt-0.5 text-lg font-semibold tabular-nums">{value}</dd>
    </div>
  );
}
