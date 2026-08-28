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

export const updateArticle = (id: number, input: ArticleInput) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/articles/${id}`, json("PUT", input));

export const updateVideo = (id: number, input: VideoInput) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/videos/${id}`, json("PUT", input));

export const setMaterialStatus = (id: number, status: MaterialStatus) =>
  apiFetch<MaterialDetail>(`/api/v1/admin/materials/${id}/status`, json("PATCH", { status }));

export const archiveMaterial = (id: number) =>
  apiFetch<void>(`/api/v1/admin/materials/${id}`, { method: "DELETE" });
