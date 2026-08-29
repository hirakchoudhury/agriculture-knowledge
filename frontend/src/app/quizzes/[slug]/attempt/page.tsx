"use client";

import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { startAttempt, submitAttempt } from "@/lib/quiz-api";
import type { AttemptView } from "@/lib/types";

export default function AttemptPage() {
  const { slug } = useParams<{ slug: string }>();
  const router = useRouter();
  const { status } = useAuth();

  const [attempt, setAttempt] = useState<AttemptView | null>(null);
  const [answers, setAnswers] = useState<Record<number, number | null>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState<number | null>(null);

  // Guards against the timer and a manual click both submitting.
  const submitted = useRef(false);

  useEffect(() => {
    if (status === "loading") return;
    if (status === "anonymous") {
      router.replace("/login");
      return;
    }

    let cancelled = false;
    void (async () => {
      try {
        const view = await startAttempt(slug);
        if (!cancelled) setAttempt(view);
      } catch (caught) {
        if (!cancelled) {
          setError(
            caught instanceof ApiError ? caught.message : "Could not start this quiz.",
          );
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [slug, status, router]);

  const send = useCallback(
    async (auto: boolean) => {
      if (!attempt || submitted.current) return;
      submitted.current = true;
      setSubmitting(true);
      setError(null);

      try {
        const result = await submitAttempt(
          attempt.attemptId,
          attempt.questions.map((question) => ({
            questionId: question.id,
            selectedOptionId: answers[question.id] ?? null,
          })),
        );
        router.replace(`/attempts/${result.attemptId}`);
      } catch (caught) {
        submitted.current = false;
        setSubmitting(false);
        setError(
          caught instanceof ApiError
            ? caught.message
            : auto
              ? "Time ran out but the answers could not be sent."
              : "Could not submit your answers.",
        );
      }
    },
    [attempt, answers, router],
  );

  // The countdown is driven from the server's expiresAt, not from a duration held
  // on the client, so a paused or skewed clock cannot buy extra time.
  useEffect(() => {
    if (!attempt?.expiresAt) return;

    const deadline = new Date(attempt.expiresAt).getTime();
    const tick = () => {
      const remaining = Math.max(0, Math.round((deadline - Date.now()) / 1000));
      setSecondsLeft(remaining);
      if (remaining === 0) {
        void send(true);
      }
    };

    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [attempt, send]);

  const answeredCount = useMemo(
    () => Object.values(answers).filter((value) => value != null).length,
    [answers],
  );

  if (status === "loading" || (!attempt && !error)) {
    return <main className="flex-1 px-6 py-16 text-sm text-muted">Loading the quiz…</main>;
  }

  if (error && !attempt) {
    return (
      <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-16">
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      </main>
    );
  }

  if (!attempt) return null;

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-6 py-12">
      <header className="flex flex-wrap items-baseline justify-between gap-3 border-b border-line pb-4">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.14em] text-accent">Quiz</p>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight">{attempt.title}</h1>
        </div>
        {secondsLeft !== null && (
          <p
            aria-live="polite"
            className={`font-mono text-sm tabular-nums ${
              secondsLeft <= 30 ? "text-danger" : "text-muted"
            }`}
          >
            {Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, "0")} left
          </p>
        )}
      </header>

      <p className="mt-4 font-mono text-xs text-muted">
        {answeredCount} of {attempt.questions.length} answered
      </p>

      <ol className="mt-8 flex flex-col gap-10">
        {attempt.questions.map((question, index) => (
          <li key={question.id}>
            <fieldset>
              <legend className="text-sm font-medium">
                <span className="font-mono text-muted">{index + 1}.</span> {question.text}
                <span className="ml-2 font-mono text-xs text-muted">
                  ({question.marks} mark{Number(question.marks) === 1 ? "" : "s"}
                  {Number(question.negativeMarks) > 0
                    ? `, −${question.negativeMarks} if wrong`
                    : ""}
                  )
                </span>
              </legend>

              <div className="mt-3 flex flex-col gap-2">
                {question.options.map((option) => (
                  <label
                    key={option.id}
                    className={`flex cursor-pointer items-start gap-3 rounded-md border px-3 py-2 text-sm transition-colors ${
                      answers[question.id] === option.id
                        ? "border-accent bg-accent-soft"
                        : "border-line bg-surface hover:border-accent"
                    }`}
                  >
                    <input
                      type="radio"
                      name={`question-${question.id}`}
                      checked={answers[question.id] === option.id}
                      onChange={() =>
                        setAnswers((current) => ({ ...current, [question.id]: option.id }))
                      }
                      className="mt-0.5"
                    />
                    <span>{option.text}</span>
                  </label>
                ))}
              </div>

              {answers[question.id] != null && (
                <button
                  type="button"
                  onClick={() =>
                    setAnswers((current) => ({ ...current, [question.id]: null }))
                  }
                  className="mt-2 text-xs text-muted underline underline-offset-4 hover:text-foreground"
                >
                  Clear answer
                </button>
              )}
            </fieldset>
          </li>
        ))}
      </ol>

      {error && (
        <p role="alert" className="mt-6 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-10 flex items-center gap-4 border-t border-line pt-6">
        <button
          type="button"
          onClick={() => void send(false)}
          disabled={submitting}
          className="rounded-md bg-accent px-5 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {submitting ? "Submitting…" : "Submit answers"}
        </button>
        <p className="text-xs text-muted">
          Unanswered questions score nothing — they do not lose you marks.
        </p>
      </div>
    </main>
  );
}
