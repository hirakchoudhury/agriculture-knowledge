/**
 * Hand-written mirrors of the backend DTOs. Kept in sync by hand deliberately:
 * a generated client would couple the frontend build to a running API.
 */

export type Role = "USER" | "ADMIN";
export type AuthProvider = "LOCAL" | "GOOGLE";

export type HealthResponse = {
  status: "UP" | "DOWN";
  service: string;
  version: string;
  profiles: string[];
  timestamp: string;
};

/** Mirrors auth/dto/UserResponse.java. Never contains a password hash. */
export type User = {
  id: number;
  email: string;
  name: string;
  avatarUrl: string | null;
  role: Role;
  provider: AuthProvider;
  createdAt: string;
};

/**
 * Mirrors auth/dto/AuthResponse.java. The refresh token is absent by design —
 * it only ever travels as an HttpOnly cookie.
 */
export type AuthResponse = {
  accessToken: string;
  expiresInSeconds: number;
  user: User;
};

/** The single error shape every failing endpoint returns. Mirrors common/ApiError.java. */
export type ApiErrorBody = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
};

/** Mirrors catalog/dto/TopicNode.java. Children is empty, never null, at a leaf. */
export type TopicNode = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  displayOrder: number;
  parentId: number | null;
  children: TopicNode[];
};

/** Mirrors catalog/dto/ExamSummary.java. */
export type ExamSummary = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  iconUrl: string | null;
  displayOrder: number;
  topicCount: number;
};

/** Mirrors catalog/dto/ExamDetail.java. */
export type ExamDetail = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  iconUrl: string | null;
  displayOrder: number;
  syllabus: TopicNode[];
};
