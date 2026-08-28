import { apiFetch } from "./api";
import type { ExamDetail, ExamSummary, TopicNode } from "./types";

export type ExamInput = {
  name: string;
  description?: string | null;
  iconUrl?: string | null;
  displayOrder: number;
};

export type TopicInput = {
  name: string;
  parentId?: number | null;
  description?: string | null;
  displayOrder: number;
};

const json = (method: string, body: unknown) => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export const listExams = () => apiFetch<ExamSummary[]>("/api/v1/exams");
export const getExam = (slug: string) => apiFetch<ExamDetail>(`/api/v1/exams/${slug}`);
export const listTopics = () => apiFetch<TopicNode[]>("/api/v1/topics");

export const createExam = (input: ExamInput) =>
  apiFetch<ExamSummary>("/api/v1/admin/exams", json("POST", input));

export const updateExam = (id: number, input: ExamInput) =>
  apiFetch<ExamSummary>(`/api/v1/admin/exams/${id}`, json("PUT", input));

export const deleteExam = (id: number) =>
  apiFetch<void>(`/api/v1/admin/exams/${id}`, { method: "DELETE" });

/** Sends the exam's complete topic set, so repeating the call changes nothing. */
export const setExamTopics = (id: number, topicIds: number[]) =>
  apiFetch<ExamDetail>(`/api/v1/admin/exams/${id}/topics`, json("PUT", { topicIds }));

export const createTopic = (input: TopicInput) =>
  apiFetch<TopicNode>("/api/v1/admin/topics", json("POST", input));

export const updateTopic = (id: number, input: TopicInput) =>
  apiFetch<TopicNode>(`/api/v1/admin/topics/${id}`, json("PUT", input));

export const deleteTopic = (id: number) =>
  apiFetch<void>(`/api/v1/admin/topics/${id}`, { method: "DELETE" });

/** Flattens the tree for pickers and tables, keeping depth for indentation. */
export function flattenTopics(
  nodes: TopicNode[],
  depth = 0,
): Array<{ node: TopicNode; depth: number }> {
  return nodes.flatMap((node) => [
    { node, depth },
    ...flattenTopics(node.children, depth + 1),
  ]);
}
