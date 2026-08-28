"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { flattenTopics } from "@/lib/admin-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

/**
 * Filters live in the URL rather than in component state, so a filtered view can
 * be bookmarked, shared and reloaded, and the back button behaves as expected.
 */
export function MaterialFilters({
  topics,
  exams,
}: {
  topics: TopicNode[];
  exams: ExamSummary[];
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [query, setQuery] = useState(params.get("q") ?? "");

  function apply(key: string, value: string) {
    const next = new URLSearchParams(params.toString());
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    // Any filter change invalidates the current page number.
    next.delete("page");
    router.push(`/materials?${next}`);
  }

  const selectClass =
    "rounded-md border border-line bg-surface px-3 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";

  return (
    <div className="mt-8 flex flex-col gap-3">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          apply("q", query.trim());
        }}
        className="flex gap-2"
      >
        <input
          aria-label="Search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search titles and summaries"
          className="flex-1 rounded-md border border-line bg-surface px-3 py-1.5 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
        />
        <button
          type="submit"
          className="rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-background"
        >
          Search
        </button>
      </form>

      <div className="flex flex-wrap gap-2">
        <select
          aria-label="Format"
          value={params.get("type") ?? ""}
          onChange={(event) => apply("type", event.target.value)}
          className={selectClass}
        >
          <option value="">Any format</option>
          <option value="ARTICLE">Articles</option>
          <option value="VIDEO">Videos</option>
        </select>

        <select
          aria-label="Difficulty"
          value={params.get("difficulty") ?? ""}
          onChange={(event) => apply("difficulty", event.target.value)}
          className={selectClass}
        >
          <option value="">Any level</option>
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCED">Advanced</option>
        </select>

        <select
          aria-label="Exam"
          value={params.get("examId") ?? ""}
          onChange={(event) => apply("examId", event.target.value)}
          className={selectClass}
        >
          <option value="">Any exam</option>
          {exams.map((exam) => (
            <option key={exam.id} value={exam.id}>
              {exam.name}
            </option>
          ))}
        </select>

        <select
          aria-label="Topic"
          value={params.get("topicId") ?? ""}
          onChange={(event) => apply("topicId", event.target.value)}
          className={selectClass}
        >
          <option value="">Any topic</option>
          {flattenTopics(topics).map(({ node, depth }) => (
            <option key={node.id} value={node.id}>
              {"— ".repeat(depth)}
              {node.name}
            </option>
          ))}
        </select>

        {[...params.keys()].some((key) => key !== "page") && (
          <button
            type="button"
            onClick={() => {
              setQuery("");
              router.push("/materials");
            }}
            className="text-sm text-muted underline underline-offset-4 hover:text-foreground"
          >
            Clear
          </button>
        )}
      </div>
    </div>
  );
}
