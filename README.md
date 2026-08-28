# Agriculture Knowledge

A two-role learning platform for agriculture competitive exams. Admins publish
articles, YouTube lessons and MCQ quizzes organised by exam and topic; learners
read, watch, attempt, like, comment and assemble their own learning paths.

**Status: phase 1 of 8 complete** — skeleton, health endpoint, CORS, and a
frontend that reads from the API. No database or authentication yet.

## Stack

| Part     | Choice                                              |
| -------- | --------------------------------------------------- |
| Backend  | Spring Boot 4.1.1, Java 21, Maven wrapper           |
| Frontend | Next.js 16.3, React 19, TypeScript, Tailwind 4      |
| Database | PostgreSQL (phase 2)                                |
| Auth     | Backend-issued JWT + Google OAuth2 (phase 2)        |
| Hosting  | Railway (API + database), Vercel (frontend)         |

## Prerequisites

- JDK 21 — `java -version` should report 21
- Node 20 or later — `node --version`

Maven is **not** required; `mvnw` downloads it on first use.

## Running locally

Two terminals. Backend first:

```
cd backend
./mvnw spring-boot:run
```

Serves <http://localhost:8080>. Verify with `curl http://localhost:8080/api/v1/health`.

Then the frontend:

```
cd frontend
npm run dev
```

Serves <http://localhost:3000>, which shows whether it can reach the API.

## Configuration

| Variable                     | Side     | Default                 | Purpose                                        |
| ---------------------------- | -------- | ----------------------- | ---------------------------------------------- |
| `PORT`                       | backend  | `8080`                  | Injected by Railway in production               |
| `APP_FRONTEND_URL`           | backend  | `http://localhost:3000` | CORS origin; OAuth redirect target from phase 2 |
| `APP_EXTRA_ALLOWED_ORIGINS`  | backend  | empty                   | Comma-separated extra CORS origins (previews)   |
| `SPRING_PROFILES_ACTIVE`     | backend  | none                    | Set to `prod` on Railway                        |
| `NEXT_PUBLIC_API_URL`        | frontend | `http://localhost:8080` | API base URL, no trailing slash                 |

Copy `frontend/.env.example` to `frontend/.env.local` to change the API URL locally.

## Layout

```
backend/            Spring Boot API
  src/main/java/com/agriknowledge/
    config/         SecurityConfig, AppProperties
    common/         ApiError and the JSON 401 / 403 handlers
    health/         HealthController
  Dockerfile        Multi-stage build used by Railway
frontend/           Next.js app
  src/app/          App Router pages
  src/lib/          api.ts (fetch wrapper), types.ts (DTO mirrors)
```

## Conventions

- All endpoints live under `/api/v1`.
- Every failure returns the same JSON shape (`common/ApiError.java`).
  A **401** means "not authenticated"; a **403** means "authenticated but not allowed".
- Public content pages are server components; anything needing the signed-in user
  is a client component, because the access token lives in browser memory.
- Database schema changes go through Flyway migrations, never `ddl-auto`.

## Testing

```
cd backend
./mvnw test
```
