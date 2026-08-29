import { apiFetch } from "./api";
import type { PathDetail, PathSummary, ProgressResponse, ProgressStatus } from "./types";

const json = (method: string, body: unknown) => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const listPaths = () => apiFetch<PathSummary[]>("/api/v1/learning-paths");

export const getPath = (id: number) => apiFetch<PathDetail>(`/api/v1/learning-paths/${id}`);

export const createPath = (title: string, description?: string | null) =>
  apiFetch<PathDetail>("/api/v1/learning-paths", json("POST", { title, description: description ?? null }));

export const renamePath = (id: number, title: string, description: string | null) =>
  apiFetch<PathDetail>(`/api/v1/learning-paths/${id}`, json("PUT", { title, description }));

export const deletePath = (id: number) =>
  apiFetch<void>(`/api/v1/learning-paths/${id}`, { method: "DELETE" });

export const addToPath = (pathId: number, materialId: number, note?: string | null) =>
  apiFetch<PathDetail>(
    `/api/v1/learning-paths/${pathId}/items`,
    json("POST", { materialId, note: note ?? null }),
  );

export const removeFromPath = (pathId: number, itemId: number) =>
  apiFetch<PathDetail>(`/api/v1/learning-paths/${pathId}/items/${itemId}`, { method: "DELETE" });

/** Sends the complete order, so a dropped request cannot half-apply it. */
export const reorderPath = (pathId: number, itemIds: number[]) =>
  apiFetch<PathDetail>(`/api/v1/learning-paths/${pathId}/items/order`, json("PUT", { itemIds }));

export const getProgress = (materialId: number) =>
  apiFetch<ProgressResponse>(`/api/v1/progress/${materialId}`);

export const setProgress = (
  materialId: number,
  status: ProgressStatus,
  lastPositionSeconds: number | null = null,
) =>
  apiFetch<ProgressResponse>(
    `/api/v1/progress/${materialId}`,
    json("PUT", { status, lastPositionSeconds }),
  );
