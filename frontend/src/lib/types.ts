/**
 * Hand-written mirrors of the backend DTOs. Kept in sync by hand deliberately:
 * a generated client would couple the frontend build to a running API.
 */

export type HealthResponse = {
  status: "UP" | "DOWN";
  service: string;
  version: string;
  profiles: string[];
  timestamp: string;
};

/** The single error shape every failing endpoint returns. Mirrors common/ApiError.java. */
export type ApiErrorBody = {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
};
