"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { MaterialFormFields, type SharedFields } from "@/components/material-form-fields";
import { YouTubeEmbed } from "@/components/youtube-embed";
import { ApiError } from "@/lib/api";
import { listExams, listTopics } from "@/lib/admin-api";
import { createVideo } from "@/lib/material-api";
import type { ExamSummary, TopicNode } from "@/lib/types";

/**
 * Mirrors the server's parser so the admin sees the preview immediately. The
 * server still does its own parsing — this is convenience, not validation.
 */
function previewVideoId(input: string): string | null {
  const trimmed = input.trim();
  const patterns = [
    /[?&]v=([A-Za-z0-9_-]{11})/,
    /youtu\.be\/([A-Za-z0-9_-]{11})/,
    /youtube(?:-nocookie)?\.com\/(?:embed|v)\/([A-Za-z0-9_-]{11})/,
    /youtube\.com\/shorts\/([A-Za-z0-9_-]{11})/,
    /youtube\.com\/live\/([A-Za-z0-9_-]{11})/,
  ];
  if (/^[A-Za-z0-9_-]{11}$/.test(trimmed)) {
    return trimmed;
  }
  for (const pattern of patterns) {
    const match = trimmed.match(pattern);
    if (match) {
      return match[1];
    }
  }
  return null;
}

export default function NewVideoPage() {
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
  const [youtubeUrl, setYoutubeUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const videoId = previewVideoId(youtubeUrl);

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
      await createVideo({
        title: shared.title,
        summary: shared.summary || null,
        difficulty: shared.difficulty,
        youtubeUrl,
        topicIds: shared.topicIds,
        examIds: shared.examIds,
      });
      router.push("/admin/materials");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not save the video.");
      setBusy(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold">New video lesson</h2>
        <p className="mt-1 text-sm text-muted">
          Saved as a draft. It stays invisible to readers until you publish it.
        </p>
      </div>

      <MaterialFormFields value={shared} onChange={setShared} topics={topics} exams={exams} />

      <div className="flex flex-col gap-1.5">
        <label htmlFor="youtubeUrl" className="text-sm font-medium">
          YouTube link
        </label>
        <input
          id="youtubeUrl"
          required
          value={youtubeUrl}
          onChange={(event) => setYoutubeUrl(event.target.value)}
          placeholder="https://www.youtube.com/watch?v=..."
          className="w-full rounded-md border border-line bg-surface px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-accent"
        />
        <p className="text-xs text-muted">
          Any shape works: watch, youtu.be, embed, shorts, or just the video id.
          Only the id is stored.
        </p>
      </div>

      {youtubeUrl.trim() !== "" && !videoId && (
        <p className="text-sm text-warn">
          That does not look like a YouTube link yet.
        </p>
      )}

      {videoId && (
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium">Preview</span>
          <YouTubeEmbed videoId={videoId} title={shared.title || "Preview"} />
          <p className="font-mono text-xs text-muted">Video id: {videoId}</p>
        </div>
      )}

      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={busy || !shared.title.trim() || !videoId}
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
