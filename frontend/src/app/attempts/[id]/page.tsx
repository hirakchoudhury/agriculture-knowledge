"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { getAttempt } from "@/lib/quiz-api";
import type { AttemptResult, ReviewOption, ReviewQuestion } from "@/lib/types";

export default function AttemptReviewPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { status } = useAuth();

  const [result, setResult] = useState<AttemptResult | null>(null);
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
        const attempt = await getAttempt(Number(id));
        if (!cancelled) setResult(attempt);
      } catch (caught) {
        if (!cancelled) {
          setError(caught instanceof ApiError ? caught.message : "Could not load that attempt.");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, status, router]);

  if (error) {
    return (
      <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-16">
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      </main>
    );
  }

  if (!result) {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Loading your result…</main>;
  }

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-12">
      <p className="font-mono text-xs uppercase tracking-[0.14em] text-muted">Result</p>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">{result.title}</h1>

      <section
        className={`mt-6 rounded-md border p-6 ${
          result.passed ? "border-accent bg-accent-soft" : "border-line bg-surface"
        }`}
      >
        <div className="flex flex-wrap items-baseline gap-x-6 gap-y-2">
          <p className="text-3xl font-semibold tabular-nums">
            {result.score}
            <span className="text-lg text-muted"> / {result.totalMarks}</span>
          </p>
          <p className="font-mono text-sm tabular-nums text-muted">{result.percentage}%</p>
          <p
            className={`font-mono text-xs uppercase tracking-wider ${
              result.passed ? "text-accent" : "text-muted"
            }`}
          >
            {result.passed ? "Passed" : `Below the ${result.passPercentage}% pass mark`}
          </p>
        </div>

        {!result.withinTimeLimit && (
          <p className="mt-3 text-xs text-warn">
            Submitted after the time limit. It still counts here, but it would not in
            the real exam.
          </p>
        )}
      </section>

      <div className="mt-6 flex gap-4 text-sm">
        <Link
          href={`/quizzes/${result.quizSlug}/attempt`}
          className="rounded-md bg-accent px-4 py-1.5 font-medium text-background"
        >
          Try again
        </Link>
        <Link
          href="/me/attempts"
          className="self-center text-muted underline underline-offset-4 hover:text-foreground"
        >
          All attempts
        </Link>
      </div>

      <h2 className="mt-12 font-mono text-xs uppercase tracking-[0.14em] text-muted">
        Review
      </h2>

      <ol className="mt-4 flex flex-col gap-8">
        {result.questions.map((question, index) => (
          <li key={question.id}>
            <QuestionReview question={question} index={index} />
          </li>
        ))}
      </ol>
    </main>
  );
}

function QuestionReview({ question, index }: { question: ReviewQuestion; index: number }) {
  const unanswered = question.selectedOptionId == null;

  return (
    <article>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-sm font-medium">
          <span className="font-mono text-muted">{index + 1}.</span> {question.text}
        </h3>
        <span
          className={`font-mono text-xs tabular-nums ${
            question.answeredCorrectly
              ? "text-accent"
              : unanswered
                ? "text-muted"
                : "text-danger"
          }`}
        >
          {Number(question.awarded) > 0 ? "+" : ""}
          {question.awarded}
        </span>
      </div>

      <ul className="mt-3 flex flex-col gap-1.5">
        {question.options.map((option) => (
          <li key={option.id}>
            <OptionRow option={option} selectedId={question.selectedOptionId} />
          </li>
        ))}
      </ul>

      {unanswered && (
        <p className="mt-2 text-xs text-muted">You left this one blank.</p>
      )}

      {question.explanation && (
        <p className="mt-3 border-l-2 border-line pl-3 text-sm text-muted">
          {question.explanation}
        </p>
      )}
    </article>
  );
}

function OptionRow({
  option,
  selectedId,
}: {
  option: ReviewOption;
  selectedId: number | null;
}) {
  const chosen = option.id === selectedId;

  // Three states worth distinguishing: the right answer, a wrong answer the learner
  // picked, and everything else.
  const style = option.correct
    ? "border-accent bg-accent-soft"
    : chosen
      ? "border-danger"
      : "border-line";

  return (
    <div className={`flex items-center justify-between gap-3 rounded-md border px-3 py-2 text-sm ${style}`}>
      <span>{option.text}</span>
      <span className="shrink-0 font-mono text-[11px] uppercase tracking-wider text-muted">
        {option.correct && "correct"}
        {!option.correct && chosen && "your answer"}
      </span>
    </div>
  );
}
