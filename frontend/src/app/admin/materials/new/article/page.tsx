"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { MaterialFormFields, type SharedFields } from "@/components/material-form-fields";
import { RichTextEditor } from "@/components/rich-text-editor";
import { ApiError } from "@/lib/api";
import { listExams, listTopics } from "@/lib/admin-api";
import { createArticle } from "@/lib/material-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

export default function NewArticlePage() {
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
  const [bodyHtml, setBodyHtml] = useState("");
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
      await createArticle({
        title: shared.title,
        summary: shared.summary || null,
        difficulty: shared.difficulty,
        bodyHtml,
        topicIds: shared.topicIds,
        examIds: shared.examIds,
      });
      // Everything is created as a draft; publishing is a separate, deliberate step.
      router.push("/admin/materials");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not save the article.");
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold">New article</h2>
        <p className="mt-1 text-sm text-muted">
          Saved as a draft. It stays invisible to readers until you publish it.
        </p>
      </div>

      <MaterialFormFields value={shared} onChange={setShared} topics={topics} exams={exams} />

      <div className="flex flex-col gap-1.5">
        <span className="text-sm font-medium">Body</span>
        <RichTextEditor value={bodyHtml} onChange={setBodyHtml} />
        <p className="text-xs text-muted">
          Formatting is cleaned against an allow-list when saved, so anything the
          editor cannot express safely is dropped.
        </p>
      </div>

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={busy || !shared.title.trim() || bodyHtml.trim().length === 0}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Saving…" : "Save draft"}
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
