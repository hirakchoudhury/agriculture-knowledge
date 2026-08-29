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

export type MaterialType = "ARTICLE" | "VIDEO" | "QUIZ";
export type MaterialStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type Difficulty = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

/** Mirrors common/PageResponse.java. */
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

/** Mirrors material/dto/MaterialSummary.java. Card view; no article body. */
export type MaterialSummary = {
  id: number;
  type: MaterialType;
  title: string;
  slug: string;
  summary: string | null;
  thumbnailUrl: string | null;
  difficulty: Difficulty;
  status: MaterialStatus;
  publishedAt: string | null;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  topicNames: string[];
  likedByMe: boolean;
};

export type TagRef = { id: number; name: string; slug: string };

/**
 * Mirrors material/dto/MaterialDetail.java. One shape for every type: bodyHtml and
 * readingMinutes are set for ARTICLE, youtubeId and durationSeconds for VIDEO.
 */
export type MaterialDetail = {
  id: number;
  type: MaterialType;
  title: string;
  slug: string;
  summary: string | null;
  thumbnailUrl: string | null;
  difficulty: Difficulty;
  status: MaterialStatus;
  authorName: string;
  publishedAt: string | null;
  updatedAt: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  topics: TagRef[];
  exams: TagRef[];
  bodyHtml: string | null;
  readingMinutes: number | null;
  youtubeId: string | null;
  durationSeconds: number | null;
  likedByMe: boolean;
};

/** Mirrors engagement/dto/LikeResponse.java. */
export type LikeResponse = { liked: boolean; likeCount: number };

/** Mirrors engagement/dto/CommentResponse.java. */
export type CommentResponse = {
  id: number;
  body: string;
  authorId: number | null;
  authorName: string | null;
  authorAvatarUrl: string | null;
  createdAt: string;
  editedAt: string | null;
  deleted: boolean;
  mine: boolean;
  parentId: number | null;
  replies: CommentResponse[];
};
