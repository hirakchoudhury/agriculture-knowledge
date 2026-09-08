import { apiFetch } from "./api";
import type {
  Difficulty,
  MaterialDetail,
  MaterialStatus,
  MaterialSummary,
  MaterialType,
  PageResponse,
} from "./types";

export type ArticleInput = {
  title: string;
  summary?: string | null;
  thumbnailUrl?: string | null;
  difficulty: Difficulty;
  bodyHtml: string;
  topicIds: number[];
  examIds: number[];
};

export type VideoInput = {
  title: string;
  summary?: string | null;
  thumbnailUrl?: string | null;
  difficulty: Difficulty;
  youtubeUrl: string;
  durationSeconds?: number | null;
  topicIds: number[];
  examIds: number[];
};

export type DocumentInput = {
  title: string;
  summary?: string | null;
  thumbnailUrl?: string | null;
  difficulty: Difficulty;
  paperYear?: number | null;
  topicIds: number[];
  examIds: number[];
};

const json = (method: string, body: unknown) => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export function listAdminMaterials(params: {
  status?: MaterialStatus | "";
  type?: MaterialType | "";
  q?: string;
  page?: number;
}) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.type) query.set("type", params.type);
  if (params.q) query.set("q", params.q);
  if (params.page) query.set("page", String(params.page));
  return apiFetch<PageResponse<MaterialSummary>>(`/api/v1/admin/materials?${query}`);
}

export const createArticle = (input: ArticleInput) =>
  apiFetch<MaterialDetail>("/api/v1/admin/materials/articles", json("POST", input));

export const createVideo = (input: VideoInput) =>
  apiFetch<MaterialDetail>("/api/v1/admin/materials/videos", json("POST", input));

/**
 * Multipart, because a PDF in JSON would have to be base64 and grow by a third.
 *
 * Content-Type is deliberately not set: the browser has to add the multipart
 * boundary itself, and setting the header by hand overwrites it with one that has
 * no boundary, which the server then cannot parse.
 */
export function createDocument(input: DocumentInput, file: File) {
  const form = new FormData();
  form.append("data", new Blob([JSON.stringify(input)], { type: "application/json" }));
  form.append("file", file);
  return apiFetch<MaterialDetail>("/api/v1/admin/materials/documents", {
    method: "POST",
    body: form,
  });
}

export const updateArticle = (id: number, input: ArticleInput) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/articles/${id}`, json("PUT", input));

export const updateVideo = (id: number, input: VideoInput) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/videos/${id}`, json("PUT", input));

export const setMaterialStatus = (id: number, status: MaterialStatus) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/${id}/status`, json("PATCH", { status }));

export const archiveMaterial = (id: number) =>
  apiFetch<void>(`/api/v1/admin/materials/${id}`, { method: "DELETE" });
