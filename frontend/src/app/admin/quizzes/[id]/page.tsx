"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { parseQuestions, toPasteFormat } from "@/lib/question-parser";
import { getAdminQuiz, replaceQuestions, type QuestionInput } from "@/lib/quiz-api";
import type { AdminQuizDetail } from "@/lib/types";

const BLANK: QuestionInput = {
  text: "",
  explanation: "",
  marks: 1,
  negativeMarks: 0,
  options: [
    { text: "", correct: true },
    { text: "", correct: false },
  ],
};

export default function QuizQuestionsPage() {
  const { id } = useParams<{ id: string }>();
  const quizId = Number(id);

  const [quiz, setQuiz] = useState<AdminQuizDetail | null>(null);
  const [questions, setQuestions] = useState<QuestionInput[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [importing, setImporting] = useState(false);
  const [importText, setImportText] = useState("");
  const [importErrors, setImportErrors] = useState<string[]>([]);

  useEffect(() => {
    void (async () => {
      try {
        const loaded = await getAdminQuiz(quizId);
        setQuiz(loaded);
        setQuestions(
          loaded.questions.map((question) => ({
            text: question.text,
            explanation: question.explanation ?? "",
            marks: Number(question.marks),
            negativeMarks: Number(question.negativeMarks),
            options: question.options.map((option) => ({
              text: option.text,
              correct: option.correct,
            })),
          })),
        );
      } catch (caught) {
        setError(caught instanceof ApiError ? caught.message : "Could not load the quiz.");
      }
    })();
  }, [quizId]);

  function update(index: number, changes: Partial<QuestionInput>) {
    setQuestions((current) =>
      current.map((question, i) => (i === index ? { ...question, ...changes } : question)),
    );
  }

  async function save() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await replaceQuestions(
        quizId,
        questions.map((question) => ({
          ...question,
          explanation: question.explanation?.trim() ? question.explanation : null,
        })),
      );
      setQuiz(saved);
      setNotice(`Saved ${saved.questions.length} question${saved.questions.length === 1 ? "" : "s"}.`);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not save the questions.");
    } finally {
      setBusy(false);
    }
  }

  function runImport() {
    const result = parseQuestions(importText);
    setImportErrors(result.errors);
    if (result.errors.length === 0) {
      setQuestions((current) => [...current, ...result.questions]);
      setImportText("");
      setImporting(false);
      setNotice(`Added ${result.questions.length} question(s). Review, then save.`);
    }
  }

  const inputClass =
    "w-full rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";
  const totalMarks = questions.reduce((sum, question) => sum + (question.marks || 0), 0);

  if (error && !quiz) {
    return <p role="alert" className="text-sm text-danger">{error}</p>;
  }
  if (!quiz) {
    return <p className="text-sm text-muted">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-wrap items-baseline justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">{quiz.title}</h2>
          <p className="font-mono text-xs text-muted">
            {questions.length} question{questions.length === 1 ? "" : "s"} · {totalMarks} marks ·{" "}
            {quiz.status}
          </p>
        </div>
        <Link
          href="/admin/materials"
          className="text-sm text-muted underline underline-offset-4 hover:text-foreground"
        >
          Back to material
        </Link>
      </header>

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => setQuestions((current) => [...current, structuredClone(BLANK)])}
          className="rounded-md border border-line px-3 py-1.5 text-sm hover:border-accent"
        >
          Add a question
        </button>
        <button
          type="button"
          onClick={() => setImporting((open) => !open)}
          className="rounded-md border border-line px-3 py-1.5 text-sm hover:border-accent"
        >
          {importing ? "Cancel import" : "Paste in many"}
        </button>
        {questions.length > 0 && (
          <button
            type="button"
            onClick={() => {
              setImportText(toPasteFormat(questions));
              setImporting(true);
            }}
            className="text-sm text-muted underline underline-offset-4 hover:text-foreground"
          >
            Export as text
          </button>
        )}
      </div>

      {importing && (
        <section className="rounded-md border border-line bg-surface p-4">
          <p className="text-sm font-medium">Paste questions</p>
          <pre className="mt-2 overflow-x-auto rounded border border-line bg-background p-3 font-mono text-xs text-muted">
{`Q: Which nutrient is most affected by soil pH?
*Phosphorus
Carbon
Silicon
E: Availability drops sharply outside pH 6 to 7.

Q: What does CEC stand for?
*Cation exchange capacity
Crop establishment coefficient`}
          </pre>
          <p className="mt-2 text-xs text-muted">
            A blank line separates questions. Mark the correct option with a leading
            asterisk. The E: line is optional. Marks default to 1 and can be edited after.
          </p>

          <textarea
            aria-label="Questions to import"
            rows={10}
            value={importText}
            onChange={(event) => setImportText(event.target.value)}
            className={`${inputClass} mt-3 font-mono`}
          />

          {importErrors.length > 0 && (
            <ul role="alert" className="mt-2 flex flex-col gap-1 text-sm text-danger">
              {importErrors.map((message) => (
                <li key={message}>{message}</li>
              ))}
            </ul>
          )}

          <button
            type="button"
            onClick={runImport}
            disabled={importText.trim().length === 0}
            className="mt-3 rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-background disabled:opacity-60"
          >
            Add these
          </button>
        </section>
      )}

      {notice && <p className="text-sm text-accent">{notice}</p>}
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}

      <ol className="flex flex-col gap-6">
        {questions.map((question, index) => (
          <li key={index} className="rounded-md border border-line bg-surface p-4">
            <div className="flex items-baseline justify-between gap-3">
              <span className="font-mono text-xs text-muted">Question {index + 1}</span>
              <button
                type="button"
                onClick={() => setQuestions((current) => current.filter((_, i) => i !== index))}
                className="text-xs text-danger underline underline-offset-4"
              >
                Remove
              </button>
            </div>

            <textarea
              aria-label={`Question ${index + 1} text`}
              rows={2}
              value={question.text}
              onChange={(event) => update(index, { text: event.target.value })}
              placeholder="Question text"
              className={`${inputClass} mt-2`}
            />

            <fieldset className="mt-3">
              <legend className="text-xs text-muted">
                Options — select the one correct answer
              </legend>
              <div className="mt-2 flex flex-col gap-2">
                {question.options.map((option, optionIndex) => (
                  <div key={optionIndex} className="flex items-center gap-2">
                    <input
                      type="radio"
                      name={`correct-${index}`}
                      checked={option.correct}
                      onChange={() =>
                        update(index, {
                          options: question.options.map((o, i) => ({
                            ...o,
                            correct: i === optionIndex,
                          })),
                        })
                      }
                      aria-label={`Mark option ${optionIndex + 1} correct`}
                    />
                    <input
                      aria-label={`Option ${optionIndex + 1} text`}
                      value={option.text}
                      onChange={(event) =>
                        update(index, {
                          options: question.options.map((o, i) =>
                            i === optionIndex ? { ...o, text: event.target.value } : o,
                          ),
                        })
                      }
                      className={inputClass}
                    />
                    {question.options.length > 2 && (
                      <button
                        type="button"
                        onClick={() =>
                          update(index, {
                            options: question.options.filter((_, i) => i !== optionIndex),
                          })
                        }
                        className="shrink-0 text-xs text-muted underline underline-offset-4"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <button
                type="button"
                onClick={() =>
                  update(index, {
                    options: [...question.options, { text: "", correct: false }],
                  })
                }
                className="mt-2 text-xs text-muted underline underline-offset-4 hover:text-foreground"
              >
                Add an option
              </button>
            </fieldset>

            <div className="mt-3 grid grid-cols-2 gap-3">
              <label className="flex flex-col gap-1 text-xs text-muted">
                Marks
                <input
                  type="number"
                  step="0.25"
                  min="0.25"
                  value={question.marks}
                  onChange={(event) => update(index, { marks: Number(event.target.value) })}
                  className={inputClass}
                />
              </label>
              <label className="flex flex-col gap-1 text-xs text-muted">
                Negative marks if wrong
                <input
                  type="number"
                  step="0.25"
                  min="0"
                  value={question.negativeMarks}
                  onChange={(event) =>
                    update(index, { negativeMarks: Number(event.target.value) })
                  }
                  className={inputClass}
                />
              </label>
            </div>

            <textarea
              aria-label={`Question ${index + 1} explanation`}
              rows={2}
              value={question.explanation ?? ""}
              onChange={(event) => update(index, { explanation: event.target.value })}
              placeholder="Explanation, shown after submission (optional)"
              className={`${inputClass} mt-3`}
            />
          </li>
        ))}
      </ol>

      <div className="sticky bottom-0 flex items-center gap-4 border-t border-line bg-background py-4">
        <button
          type="button"
          onClick={save}
          disabled={busy || questions.length === 0}
          className="rounded-md bg-accent px-5 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Saving…" : "Save questions"}
        </button>
        <p className="text-xs text-muted">
          Saving replaces the whole set. Attempts already submitted keep the score they
          were marked against.
        </p>
      </div>
    </div>
  );
}
