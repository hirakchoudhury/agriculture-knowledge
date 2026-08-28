# Agriculture Knowledge

A two-role learning platform for agriculture competitive exams. Admins publish
articles, YouTube lessons and MCQ quizzes organised by exam and topic; learners
read, watch, attempt, like, comment and assemble their own learning paths.

**Status: phases 1–2 of 8 complete** — skeleton, database, and full email/password
plus Google authentication. No learning content yet.

## Stack

| Part     | Choice                                             |
| -------- | -------------------------------------------------- |
| Backend  | Spring Boot 4.1.1, Java 21, Maven wrapper          |
| Frontend | Next.js 16.3, React 19, TypeScript, Tailwind 4     |
| Database | PostgreSQL (Neon in development), Flyway migrations |
| Auth     | Backend-issued JWT + Google OAuth2                 |
| Hosting  | Railway (API), Vercel (frontend)                   |

Two things differ from most tutorials you will find: Boot 4 renamed the starters
(`spring-boot-starter-webmvc`, not `-web`) and moved to **Jackson 3** under
`tools.jackson`, not `com.fasterxml.jackson`.

## Prerequisites

- JDK 21 — `java -version` should report 21
- Node 20 or later — `node --version`
- A PostgreSQL database (a free [Neon](https://neon.tech) project is enough)

Maven is **not** required; `mvnw` downloads it on first use.

## First-time setup

```
cd backend/src/main/resources
cp application-local.yml.example application-local.yml
```

Fill in your database URL, username and password, and generate a JWT secret with
`openssl rand -base64 32`. That file is gitignored and must never be committed.

Neon's connection string looks like
`postgresql://USER:PASSWORD@HOST/DB?sslmode=require&channel_binding=require`.
Drop `channel_binding` — it is a libpq option the JDBC driver rejects — and prefix
the host with `jdbc:postgresql://`.

## Running locally

Two terminals. Backend first:

```
cd backend
./mvnw spring-boot:run
```

Serves <http://localhost:8080>. Flyway applies migrations on startup.

Then the frontend:

```
cd frontend
npm run dev
```

Serves <http://localhost:3000>.

## Google sign-in (optional)

Sign-in with Google switches itself on only when credentials are present, so the
application starts fine without them. To enable it:

1. In the Google Cloud Console, create an **OAuth 2.0 Client ID** of type *Web
   application*.
2. Add this authorised redirect URI — note it points at the **API**, not the
   frontend, which is the single most common mistake here:

   ```
   http://localhost:8080/login/oauth2/code/google
   ```

   In production, the same path on your Railway domain.
3. Set these environment variables before starting the backend:

   ```
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
   ```

## How authentication works

- **Access token**: a 15-minute HMAC-signed JWT, returned in the response body and
  held only in browser memory. Never in `localStorage`, so a stored-XSS bug in a
  future comment feature cannot steal sessions.
- **Refresh token**: an opaque random string in an `HttpOnly` cookie scoped to
  `/api/v1/auth`, stored server-side as a SHA-256 hash. Rotated on every use.
- **Replay defence**: presenting an already-revoked refresh token revokes every
  session for that account, in its own transaction so the rejection cannot roll it back.
- **Google**: handled entirely by the backend, which mints its own JWT and redirects
  to `/auth/callback#token=...`. The token rides in the URL *fragment*, which browsers
  never send to servers, keeping it out of access logs and `Referer` headers.
- **Route guarding is client-side.** The refresh cookie belongs to the API's origin,
  so the Next.js server never receives it and cannot tell a signed-in visitor from an
  anonymous one. Protected pages wait for the auth context and redirect.

## Endpoints

| Method | Path                     | Access | Notes                                  |
| ------ | ------------------------ | ------ | -------------------------------------- |
| GET    | `/api/v1/health`         | public | Liveness plus CORS proof               |
| POST   | `/api/v1/auth/register`  | public | 201, sets the refresh cookie           |
| POST   | `/api/v1/auth/login`     | public |                                        |
| POST   | `/api/v1/auth/refresh`   | cookie | Rotates the refresh token              |
| POST   | `/api/v1/auth/logout`    | cookie | 204, revokes and clears the cookie     |
| GET    | `/api/v1/users/me`       | user   | Live profile row, not the token claims |
| PATCH  | `/api/v1/users/me`       | user   | Name and avatar                        |

## Layout

```
backend/            Spring Boot API
  src/main/java/com/agriknowledge/
    config/         SecurityConfig, AppProperties
    common/         ApiError, exception handlers, JSON 401 / 403
    auth/           AuthService, controller, refresh tokens, JWT, OAuth2
    user/           User entity, roles, profile endpoints
    health/         HealthController
  src/main/resources/db/migration/   Flyway migrations
  scripts/auth_smoke.py              End-to-end auth check
  Dockerfile        Multi-stage build used by Railway
frontend/           Next.js app
  src/app/          App Router pages
  src/lib/          api.ts, auth-context.tsx, token-store.ts, types.ts
```

## Conventions

- All endpoints live under `/api/v1`.
- Every failure returns the same JSON shape (`common/ApiError.java`).
  A **401** means "not authenticated"; a **403** means "authenticated but not allowed".
- Public content pages are server components; anything needing the signed-in user
  is a client component, because the access token lives in browser memory.
- Schema changes go through Flyway migrations, never `ddl-auto`. Migrations are
  append-only: fix a mistake with a new file, never by editing an applied one.

## Testing

```
cd backend
./mvnw test                  # unit and slice tests
python scripts/auth_smoke.py # end-to-end, needs the API running
```
