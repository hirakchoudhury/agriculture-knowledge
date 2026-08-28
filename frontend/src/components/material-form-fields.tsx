"use client";

import { flattenTopics } from "@/lib/admin-api";
import type { Difficulty, ExamSummary, TopicNode } from "@/lib/types";

export type SharedFields = {
  title: string;
  summary: string;
  difficulty: Difficulty;
  topicIds: number[];
  examIds: number[];
};

/**
 * The title, summary, level and tagging that every material type shares. Articles
 * and videos differ only in what they add below this.
 */
export function MaterialFormFields({
  value,
  onChange,
  topics,
  exams,
}: {
  value: SharedFields;
  onChange: (next: SharedFields) => void;
  topics: TopicNode[];
  exams: ExamSummary[];
}) {
  const set = <K extends keyof SharedFields>(key: K, next: SharedFields[K]) =>
    onChange({ ...value, [key]: next });

  const toggle = (key: "topicIds" | "examIds", id: number) => {
    const current = value[key];
    set(key, current.includes(id) ? current.filter((x) => x !== id) : [...current, id]);
  };

  const inputClass =
    "w-full rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";

  return (
    <>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="title" className="text-sm font-medium">
          Title
        </label>
        <input
          id="title"
          required
          value={value.title}
          onChange={(event) => set("title", event.target.value)}
          className={inputClass}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="summary" className="text-sm font-medium">
          Summary
        </label>
        <textarea
          id="summary"
          rows={2}
          value={value.summary}
          onChange={(event) => set("summary", event.target.value)}
          className={inputClass}
        />
        <p className="text-xs text-muted">
          Shown on cards and in search results. One or two sentences.
        </p>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="difficulty" className="text-sm font-medium">
          Level
        </label>
        <select
          id="difficulty"
          value={value.difficulty}
          onChange={(event) => set("difficulty", event.target.value as Difficulty)}
          className={inputClass}
        >
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
        </select>
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="text-sm font-medium">Topics</legend>
        {topics.length === 0 ? (
          <p className="text-sm text-muted">
            No topics yet. Add some on the Topics tab first.
          </p>
        ) : (
          <ul className="flex max-h-56 flex-col gap-1 overflow-y-auto rounded-md border border-line bg-surface p-3">
            {flattenTopics(topics).map(({ node, depth }) => (
              <li key={node.id} style={{ paddingLeft: depth * 16 }}>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={value.topicIds.includes(node.id)}
                    onChange={() => toggle("topicIds", node.id)}
                  />
                  {node.name}
                </label>
              </li>
            ))}
          </ul>
        )}
      </fieldset>

      <fieldset className="flex flex-col gap-2">
        <legend className="text-sm font-medium">Exams</legend>
        {exams.length === 0 ? (
          <p className="text-sm text-muted">No exams yet.</p>
        ) : (
          <ul className="flex flex-col gap-1 rounded-md border border-line bg-surface p-3">
            {exams.map((exam) => (
              <li key={exam.id}>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={value.examIds.includes(exam.id)}
                    onChange={() => toggle("examIds", exam.id)}
                  />
                  {exam.name}
                </label>
              </li>
            ))}
          </ul>
        )}
      </fieldset>
    </>
  );
}
