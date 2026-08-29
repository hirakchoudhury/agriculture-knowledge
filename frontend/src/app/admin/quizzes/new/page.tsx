"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { MaterialFormFields, type SharedFields } from "@/components/material-form-fields";
import { ApiError } from "@/lib/api";
import { listExams, listTopics } from "@/lib/admin-api";
import { createQuiz } from "@/lib/quiz-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

export default function NewQuizPage() {
  const router = useRouter();
  const [topics, setTopics] = useState<TopicNode[]>([]);
  const [exams, setExams] = useState<ExamSummary[]>([]);

  const [shared, setShared] = useState<SharedFields>({
    title: "",
    summary: "",
    difficulty: "BEGINNER",
    topicIds: [],
    examIds: [],
  });
  const [timed, setTimed] = useState(true);
  const [minutes, setMinutes] = useState(10);
  const [passPercentage, setPassPercentage] = useState(60);
  const [shuffle, setShuffle] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        const [nextTopics, nextExams] = await Promise.all([listTopics(), listExams()]);
        setTopics(nextTopics);
        setExams(nextExams);
      } catch {
        setError("Could not load topics and exams.");
      }
    })();
  }, []);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const quiz = await createQuiz({
        title: shared.title,
        summary: shared.summary || null,
        difficulty: shared.difficulty,
        timeLimitSeconds: timed ? minutes * 60 : null,
        passPercentage,
        shuffleQuestions: shuffle,
        topicIds: shared.topicIds,
        examIds: shared.examIds,
      });
      // Straight into the question builder: a quiz with no questions is unusable.
      router.push(`/admin/quizzes/${quiz.id}`);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not create the quiz.");
      setBusy(false);
    }
  }

  const inputClass =
    "w-full rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";

  return (
    <form onSubmit={handleSubmit} className="flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold">New quiz</h2>
        <p className="mt-1 text-sm text-muted">
          Set it up here, then add the questions. It stays a draft until you publish it.
        </p>
      </div>

      <MaterialFormFields value={shared} onChange={setShared} topics={topics} exams={exams} />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="pass" className="text-sm font-medium">
            Pass mark (%)
          </label>
          <input
            id="pass"
            type="number"
            min={0}
            max={100}
            value={passPercentage}
            onChange={(event) => setPassPercentage(Number(event.target.value))}
            className={inputClass}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="minutes" className="text-sm font-medium">
            Time limit
          </label>
          <div className="flex items-center gap-3">
            <input
              id="minutes"
              type="number"
              min={1}
              disabled={!timed}
              value={minutes}
              onChange={(event) => setMinutes(Number(event.target.value))}
              className={`${inputClass} disabled:opacity-50`}
            />
            <span className="shrink-0 text-sm text-muted">min</span>
          </div>
          <label className="flex items-center gap-2 text-xs text-muted">
            <input
              type="checkbox"
              checked={!timed}
              onChange={(event) => setTimed(!event.target.checked)}
            />
            No time limit
          </label>
        </div>
      </div>

      <label className="flex items-center gap-2 text-sm">
        <input
          type="checkbox"
          checked={shuffle}
          onChange={(event) => setShuffle(event.target.checked)}
        />
        Shuffle the question order for each learner
      </label>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={busy || !shared.title.trim()}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Creating…" : "Create and add questions"}
        </button>
        <button
          type="button"
          onClick={() => router.push("/admin/materials")}
          className="text-sm text-muted underline underline-offset-4"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
