import { apiFetch } from "./api";
import { fetchPublic } from "./public-api";
import type {
  AdminQuizDetail,
  AttemptResult,
  AttemptSummary,
  AttemptView,
  Difficulty,
  PageResponse,
  QuizSummary,
} from "./types";

const json = (method: string, body: unknown) => ({
  method,
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

// --- learner -----------------------------------------------------------------

export const getQuizSummary = (slug: string) =>
  fetchPublic<QuizSummary>(`/api/v1/quizzes/${slug}`);

/** Starts an attempt, or resumes the one already open. */
export const startAttempt = (slug: string) =>
  apiFetch<AttemptView>(`/api/v1/quizzes/${slug}/attempts`, { method: "POST" });

export const submitAttempt = (
  attemptId: number,
  answers: Array<{ questionId: number; selectedOptionId: number | null }>,
) => apiFetch<AttemptResult>(`/api/v1/attempts/${attemptId}/submit`, json("POST", { answers }));

export const getAttempt = (attemptId: number) =>
  apiFetch<AttemptResult>(`/api/v1/attempts/${attemptId}`);

export const getMyAttempts = (page = 0) =>
  apiFetch<PageResponse<AttemptSummary>>(`/api/v1/users/me/attempts?page=${page}`);

// --- admin -------------------------------------------------------------------

export type QuizInput = {
  title: string;
  summary?: string | null;
  difficulty: Difficulty;
  timeLimitSeconds: number | null;
  passPercentage: number;
  shuffleQuestions: boolean;
  topicIds: number[];
  examIds: number[];
};

export type QuestionInput = {
  text: string;
  explanation?: string | null;
  imageUrl?: string | null;
  marks: number;
  negativeMarks: number;
  options: Array<{ text: string; correct: boolean }>;
};

export const createQuiz = (input: QuizInput) =>
  apiFetch<AdminQuizDetail>("/api/v1/admin/quizzes", json("POST", input));

export const getAdminQuiz = (id: number) =>
  apiFetch<AdminQuizDetail>(`/api/v1/admin/quizzes/${id}`);

export const updateQuiz = (id: number, input: QuizInput) =>
  apiFetch<AdminQuizDetail>(`/api/v1/admin/quizzes/${id}`, json("PUT", input));

/** Replaces every question at once, which is what makes a paste-in import work. */
export const replaceQuestions = (id: number, questions: QuestionInput[]) =>
  apiFetch<AdminQuizDetail>(`/api/v1/admin/quizzes/${id}/questions`, json("PUT", { questions }));
