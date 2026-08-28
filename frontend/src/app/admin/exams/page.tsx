"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import {
  createExam,
  deleteExam,
  flattenTopics,
  getExam,
  listExams,
  listTopics,
  setExamTopics,
  updateExam,
} from "@/lib/admin-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

export default function AdminExamsPage() {
  const [exams, setExams] = useState<ExamSummary[]>([]);
  const [topics, setTopics] = useState<TopicNode[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [assigningId, setAssigningId] = useState<number | null>(null);
  const [selectedTopicIds, setSelectedTopicIds] = useState<Set<number>>(new Set());

  const refresh = useCallback(async () => {
    try {
      const [nextExams, nextTopics] = await Promise.all([listExams(), listTopics()]);
      setExams(nextExams);
      setTopics(nextTopics);
      setError(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not load the catalogue.");
    } finally {
      setLoading(false);
    }
  }, []);

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

  async function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    await run(async () => {
      await createExam({ name, description: description || null, displayOrder: exams.length });
      setName("");
      setDescription("");
    });
  }

  async function openAssign(exam: ExamSummary) {
    setAssigningId(exam.id);
    setError(null);
    try {
      const detail = await getExam(exam.slug);
      // The syllabus arrives as a tree; the picker needs a flat set of ids.
      setSelectedTopicIds(new Set(flattenTopics(detail.syllabus).map((row) => row.node.id)));
    } catch {
      setSelectedTopicIds(new Set());
    }
  }

  const flatTopics = flattenTopics(topics);

  return (
    <div className="flex flex-col gap-10">
      <section>
        <h2 className="text-lg font-semibold">Add an exam</h2>
        <form onSubmit={handleCreate} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-start">
          <input
            aria-label="Exam name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="ICAR JRF"
            className="flex-1 rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <input
            aria-label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Short description (optional)"
            className="flex-1 rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
          />
          <button
            type="submit"
            disabled={busy}
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
          >
            Add
          </button>
        </form>
      </section>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <section>
        <h2 className="text-lg font-semibold">
          Exams {!loading && <span className="text-muted">({exams.length})</span>}
        </h2>

        {loading && <p className="mt-4 text-sm text-muted">Loading…</p>}
        {!loading && exams.length === 0 && (
          <p className="mt-4 text-sm text-muted">No exams yet. Add one above.</p>
        )}

        <ul className="mt-4 flex flex-col gap-3">
          {exams.map((exam) => (
            <li key={exam.id} className="rounded-md border border-line bg-surface p-4">
              {editingId === exam.id ? (
                <EditExamForm
                  exam={exam}
                  busy={busy}
                  onCancel={() => setEditingId(null)}
                  onSave={async (input) => {
                    await run(() => updateExam(exam.id, input));
                    setEditingId(null);
                  }}
                />
              ) : (
                <div className="flex flex-wrap items-baseline justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="font-medium">{exam.name}</h3>
                    <p className="font-mono text-xs text-muted">/exams/{exam.slug}</p>
                    {exam.description && (
                      <p className="mt-1 text-sm text-muted">{exam.description}</p>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-3 text-sm">
                    <span className="font-mono text-xs text-muted">
                      {exam.topicCount} {exam.topicCount === 1 ? "topic" : "topics"}
                    </span>
                    <button
                      type="button"
                      onClick={() => void openAssign(exam)}
                      className="text-accent underline underline-offset-4"
                    >
                      Topics
                    </button>
                    <button
                      type="button"
                      onClick={() => setEditingId(exam.id)}
                      className="text-muted underline underline-offset-4 hover:text-foreground"
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => void run(() => deleteExam(exam.id))}
                      className="text-danger underline underline-offset-4"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              )}

              {assigningId === exam.id && (
                <div className="mt-4 border-t border-line pt-4">
                  <p className="text-sm font-medium">Topics in this syllabus</p>
                  <p className="mt-1 text-xs text-muted">
                    A topic can belong to any number of exams. Ticking it here does not
                    remove it from another.
                  </p>

                  {flatTopics.length === 0 ? (
                    <p className="mt-3 text-sm text-muted">
                      No topics exist yet. Create some on the Topics tab first.
                    </p>
                  ) : (
                    <ul className="mt-3 flex max-h-72 flex-col gap-1 overflow-y-auto">
                      {flatTopics.map(({ node, depth }) => (
                        <li key={node.id} style={{ paddingLeft: depth * 18 }}>
                          <label className="flex items-center gap-2 text-sm">
                            <input
                              type="checkbox"
                              checked={selectedTopicIds.has(node.id)}
                              onChange={(e) => {
                                const next = new Set(selectedTopicIds);
                                if (e.target.checked) {
                                  next.add(node.id);
                                } else {
                                  next.delete(node.id);
                                }
                                setSelectedTopicIds(next);
                              }}
                            />
                            {node.name}
                          </label>
                        </li>
                      ))}
                    </ul>
                  )}

                  <div className="mt-4 flex gap-3">
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() =>
                        void run(async () => {
                          await setExamTopics(exam.id, [...selectedTopicIds]);
                          setAssigningId(null);
                        })
                      }
                      className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-background disabled:opacity-60"
                    >
                      Save syllabus
                    </button>
                    <button
                      type="button"
                      onClick={() => setAssigningId(null)}
                      className="text-sm text-muted underline underline-offset-4"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

function EditExamForm({
  exam,
  busy,
  onSave,
  onCancel,
}: {
  exam: ExamSummary;
  busy: boolean;
  onSave: (input: { name: string; description: string | null; displayOrder: number }) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState(exam.name);
  const [description, setDescription] = useState(exam.description ?? "");

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSave({ name, description: description || null, displayOrder: exam.displayOrder });
      }}
      className="flex flex-col gap-3 sm:flex-row"
    >
      <input
        aria-label="Exam name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        className="flex-1 rounded-md border border-line bg-background px-3 py-2 text-sm"
      />
      <input
        aria-label="Description"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        className="flex-1 rounded-md border border-line bg-background px-3 py-2 text-sm"
      />
      <button
        type="submit"
        disabled={busy}
        className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-background disabled:opacity-60"
      >
        Save
      </button>
      <button
        type="button"
        onClick={onCancel}
        className="text-sm text-muted underline underline-offset-4"
      >
        Cancel
      </button>
    </form>
  );
}
