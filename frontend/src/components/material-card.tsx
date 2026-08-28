import Link from "next/link";
import type { MaterialSummary } from "@/lib/types";

const TYPE_LABEL: Record<string, string> = {
  ARTICLE: "Article",
  VIDEO: "Video",
  QUIZ: "Quiz",
};

export function MaterialCard({ material }: { material: MaterialSummary }) {
  return (
    <Link
      href={`/materials/${material.slug}`}
      className="flex h-full flex-col rounded-md border border-line bg-surface p-5 transition-colors hover:border-accent"
    >
      <div className="flex items-center gap-2">
        <span className="font-mono text-[11px] uppercase tracking-[0.1em] text-accent">
          {TYPE_LABEL[material.type] ?? material.type}
        </span>
        {material.status !== "PUBLISHED" && (
          <span className="rounded bg-warn/15 px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-wider text-warn">
            {material.status}
          </span>
        )}
      </div>

      <h3 className="mt-2 font-semibold leading-snug">{material.title}</h3>

      {material.summary && (
        <p className="mt-1.5 line-clamp-3 text-sm text-muted">{material.summary}</p>
      )}

      {material.topicNames.length > 0 && (
        <p className="mt-3 line-clamp-1 text-xs text-muted">
          {material.topicNames.join(" · ")}
        </p>
      )}

      <p className="mt-auto pt-3 font-mono text-[11px] text-muted">
        {material.viewCount} {material.viewCount === 1 ? "view" : "views"}
      </p>
    </Link>
  );
}
