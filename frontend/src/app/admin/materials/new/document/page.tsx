"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { MaterialFormFields, type SharedFields } from "@/components/material-form-fields";
import { ApiError } from "@/lib/api";
import { listExams, listTopics } from "@/lib/admin-api";
import { createDocument } from "@/lib/material-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

/** Mirrors PdfFiles.MAX_BYTES. Checked here only to fail fast, never as the real limit. */
const MAX_BYTES = 25 * 1024 * 1024;

function formatSize(bytes: number) {
  return bytes < 1024 * 1024
    ? `${Math.round(bytes / 1024)} KB`
    : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function NewDocumentPage() {
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
  const [paperYear, setPaperYear] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);
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

  const tooBig = file !== null && file.size > MAX_BYTES;

  function handleFile(event: React.ChangeEvent<HTMLInputElement>) {
    setError(null);
    setFile(event.target.files?.[0] ?? null);
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      await createDocument(
        {
          title: shared.title,
          summary: shared.summary || null,
          difficulty: shared.difficulty,
          paperYear: paperYear ? Number(paperYear) : null,
          topicIds: shared.topicIds,
          examIds: shared.examIds,
        },
        file,
      );
      router.push("/admin/materials");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not upload the paper.");
      setBusy(false);
    }
  }

  const inputClass =
    "w-full rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent";

  return (
    <form onSubmit={handleSubmit} className="flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold">New paper</h2>
        <p className="mt-1 text-sm text-muted">
          Upload a PDF, usually a previous year paper. Saved as a draft; it stays
          invisible to readers until you publish it.
        </p>
      </div>

      <MaterialFormFields value={shared} onChange={setShared} topics={topics} exams={exams} />

      <div className="flex flex-col gap-1.5">
        <label htmlFor="paperYear" className="text-sm font-medium">
          Year
        </label>
        <input
          id="paperYear"
          type="number"
          min={1900}
          max={2200}
          value={paperYear}
          onChange={(event) => setPaperYear(event.target.value)}
          placeholder="2024"
          className={`${inputClass} max-w-40`}
        />
        <p className="text-xs text-muted">
          The year the paper is from. Leave it empty for anything that is not
          year-specific — notes, a syllabus, a formula sheet.
        </p>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="file" className="text-sm font-medium">
          PDF
        </label>
        <input
          id="file"
          type="file"
          accept="application/pdf,.pdf"
          required
          onChange={handleFile}
          className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm file:mr-3 file:rounded file:border-0 file:bg-accent-soft file:px-3 file:py-1 file:text-sm file:text-accent"
        />
        {file && (
          <p className={`text-xs ${tooBig ? "text-danger" : "text-muted"}`}>
            {file.name} · {formatSize(file.size)}
            {tooBig && " — over the 25 MB limit"}
          </p>
        )}
        <p className="text-xs text-muted">
          Up to 25 MB. The file is checked on the server for a real PDF signature,
          so renaming something else to .pdf will be rejected.
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
          disabled={busy || !shared.title.trim() || !file || tooBig}
          className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-background disabled:opacity-60"
        >
          {busy ? "Uploading…" : "Save draft"}
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
