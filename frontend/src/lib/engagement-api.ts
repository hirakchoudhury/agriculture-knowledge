import { apiFetch } from "./api";
import type { CommentResponse, LikeResponse, PageResponse } from "./types";

const json = (method: string, body: unknown) => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const getLikeStatus = (materialId: number) =>
  apiFetch<LikeResponse>(`/api/v1/materials/${materialId}/like`);

export const like = (materialId: number) =>
  apiFetch<LikeResponse>(`/api/v1/materials/${materialId}/like`, { method: "POST" });

export const unlike = (materialId: number) =>
  apiFetch<LikeResponse>(`/api/v1/materials/${materialId}/like`, { method: "DELETE" });

export const listComments = (materialId: number, page = 0) =>
  apiFetch<PageResponse<CommentResponse>>(
    `/api/v1/materials/${materialId}/comments?page=${page}`,
  );

export const addComment = (materialId: number, body: string, parentId?: number) =>
  apiFetch<CommentResponse>(
    `/api/v1/materials/${materialId}/comments`,
    json("POST", { body, parentId: parentId ?? null }),
  );

export const editComment = (commentId: number, body: string) =>
  apiFetch<CommentResponse>(`/api/v1/comments/${commentId}`, json("PATCH", { body }));

export const deleteComment = (commentId: number) =>
  apiFetch<void>(`/api/v1/comments/${commentId}`, { method: "DELETE" });
