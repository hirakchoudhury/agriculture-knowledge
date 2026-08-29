# Agriculture Knowledge

A two-role learning platform for agriculture competitive exams. Admins publish
articles, YouTube lessons and MCQ quizzes organised by exam and topic; learners
read, watch, attempt, like, comment and assemble their own learning paths.

**Status: phases 1–6 of 8 complete** — skeleton, database, authentication, the
exam/topic taxonomy, articles and video lessons, likes and comments, and MCQ
quizzes with server-side marking. Learning paths and search are still to come.

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

## Making yourself an admin

There is deliberately no public route to becoming an admin, and a Flyway seed
cannot do it either without hard-coding an email address into version control.
Instead, list the addresses in configuration:

1. Sign up normally through the app.
2. Add the address to `application-local.yml` (or set `APP_BOOTSTRAP_ADMIN_EMAILS`
   in production):

   ```yaml
   app:
     bootstrap-admin-emails: you@example.com
   ```

3. Restart the API. It logs `Promoted you@example.com to ADMIN`, and an **Admin**
   link appears in the header.

The account must exist first — promoting an unknown address logs a warning and
does nothing.

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
| GET    | `/api/v1/exams`          | public | With topic counts                      |
| GET    | `/api/v1/exams/{slug}`   | public | Includes the syllabus tree             |
| GET    | `/api/v1/topics`         | public | The whole topic forest                 |
| POST   | `/api/v1/admin/exams`    | admin  | Also PUT and DELETE by id              |
| PUT    | `/api/v1/admin/exams/{id}/topics` | admin | Replaces the whole topic set  |
| POST   | `/api/v1/admin/topics`   | admin  | Also PUT and DELETE by id              |
| GET    | `/api/v1/materials`      | public | Filter by type, difficulty, topic, exam, `q` |
| GET    | `/api/v1/materials/{slug}` | public | Drafts are 404 unless you are an admin |
| GET    | `/api/v1/admin/materials` | admin | Includes drafts and archived           |
| POST   | `/api/v1/admin/materials/articles` | admin | Also PUT by id                |
| POST   | `/api/v1/admin/materials/videos`   | admin | Also PUT by id                |
| PATCH  | `/api/v1/admin/materials/{id}/status` | admin | Publish, unpublish, archive |
| GET    | `/api/v1/materials/{id}/like` | public | Reports the caller's own like state |
| POST   | `/api/v1/materials/{id}/like` | user | Idempotent; DELETE to undo    |
| GET    | `/api/v1/materials/{id}/comments` | public | Threads, replies nested one level |
| POST   | `/api/v1/materials/{id}/comments` | user | Rate limited                |
| PATCH  | `/api/v1/comments/{id}`  | author | Author only, never an admin       |
| DELETE | `/api/v1/comments/{id}`  | author/admin | Soft delete                 |
| GET    | `/api/v1/quizzes/{slug}` | public | Counts and marks, never the questions |
| POST   | `/api/v1/quizzes/{slug}/attempts` | user | Starts or resumes an attempt |
| POST   | `/api/v1/attempts/{id}/submit` | owner | Marked server-side          |
| GET    | `/api/v1/attempts/{id}`  | owner  | Review with explanations          |
| GET    | `/api/v1/users/me/attempts` | user | Attempt history                 |
| POST   | `/api/v1/admin/quizzes`  | admin  | Also PUT by id                    |
| PUT    | `/api/v1/admin/quizzes/{id}/questions` | admin | Replaces the whole set |

## Layout

```
backend/            Spring Boot API
  src/main/java/com/agriknowledge/
    config/         SecurityConfig, AppProperties
    common/         ApiError, exception handlers, JSON 401 / 403
    auth/           AuthService, controller, refresh tokens, JWT, OAuth2
    user/           User entity, roles, profile endpoints
    catalog/        Exams, the topic tree, and the admin CRUD behind them
    material/       Articles and videos, tagging, publishing, HTML sanitising
    engagement/     Likes, comment threads, moderation, rate limiting
    quiz/           Quizzes, questions, attempts and marking
    health/         HealthController
  src/main/resources/db/migration/   Flyway migrations
  scripts/auth_smoke.py              End-to-end auth check
  scripts/catalog_smoke.py           End-to-end taxonomy check
  scripts/materials_smoke.py         End-to-end article and video check
  scripts/engagement_smoke.py        End-to-end like and comment check
  scripts/quizzes_smoke.py           End-to-end quiz and marking check
  Dockerfile        Multi-stage build used by Railway
frontend/           Next.js app
  src/app/          App Router pages
  src/lib/          api.ts, auth-context.tsx, token-store.ts, types.ts
```

## How engagement works

- **Liking is idempotent.** Pressing like twice is the same as once, which matters
  when an optimistic UI retries after a dropped connection. A unique constraint on
  `(user_id, material_id)` settles the race; the application check in front of it
  is only an optimisation.
- **Counts are denormalised** onto the material row and moved by a delta in the
  same transaction as the like or comment. A listing of twenty cards would
  otherwise run two aggregates per card.
- **Comments are one level deep.** Replying to a reply is refused, because
  unbounded nesting has no sensible rendering.
- **Deleting a comment is a soft delete.** The row survives so replies underneath
  it keep their anchor, but the text and the author's name are dropped. A deleted
  comment is only shown while it still has a *visible* reply — otherwise it
  disappears entirely rather than leaving a stub.
- **An admin can delete any comment but cannot edit one.** Removing something
  objectionable is moderation; rewriting it puts words in someone's mouth.
- **Comment creation is rate limited** to five per minute per account. The limiter
  is in-memory and therefore per-instance: on more than one API container this
  belongs in Redis instead.

Two frontend consequences of the token living in browser memory:

- The material page is server-rendered *without* the viewer's token, so the first
  HTML always says "not liked". The like button corrects itself on mount with one
  small request rather than re-fetching the whole material.
- Components that need to know who the viewer is must wait for the auth context to
  settle before fetching. A request sent before the session is restored carries no
  token, and the server correctly answers as if for an anonymous reader.

## How quizzes work

A quiz is a third kind of material, sharing the `materials` row, so it appears in
the same feed, carries the same tags, and uses the same draft/publish workflow.

**The answer key never reaches a learner who has not submitted.** This is enforced
structurally rather than by remembering to strip a field: the DTO used for taking a
quiz (`AttemptOption`) has no `correct` field at all, and no explanation either.
`scripts/quizzes_smoke.py` asserts that the strings `correct`, `iscorrect` and
`explanation` are absent from the raw take-the-quiz payload — a test that fails if
anyone ever widens that type.

- **Marking is server-side.** A right answer earns the marks, a wrong one subtracts
  the negative marks, and a blank scores zero rather than being penalised.
- **Decimals throughout**, because negative marking in Indian competitive exams is
  routinely fractional. Scores are `BigDecimal` server-side; the client only displays.
- **An option id belonging to a different question is rejected**, not silently
  treated as unanswered, because that means the client is confused.
- **Attempts are resumed, not restarted.** Reloading mid-quiz returns the attempt
  already open, and the countdown is computed from the server's `expiresAt` so a
  skewed client clock cannot buy extra time.
- **The time limit is reported, not enforced.** This is practice: a learner who runs
  over only shortchanges their own exam simulation, and rejecting the submission
  would throw away work over a network hiccup. The result carries `withinTimeLimit`.
- **Past results are immutable.** Each attempt stores its own score and total, so
  editing a quiz afterwards cannot rewrite what someone already scored.
- **Questions are replaced as a whole set**, which is what makes the paste-in bulk
  import a single request. Exactly one option must be correct, checked server-side.

The admin question builder accepts a plain-text format, because typing fifty MCQs
through a form one at a time is punishing:

```
Q: Which nutrient is most affected by soil pH?
*Phosphorus
Carbon
Silicon
E: Availability drops sharply outside pH 6 to 7.
```

A blank line separates questions and an asterisk marks the correct option.

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
python scripts/auth_smoke.py     # end-to-end auth, needs the API running
python scripts/catalog_smoke.py   # end-to-end taxonomy, needs an admin account
python scripts/materials_smoke.py # end-to-end material, needs an admin account
python scripts/engagement_smoke.py # end-to-end likes and comments
python scripts/quizzes_smoke.py    # end-to-end quizzes and marking
```

Each smoke script archives the material it creates, so running them does not leave
test rows on the public library page.

## How material works

Articles, videos and (from phase 6) quizzes share one `materials` row plus a
type-specific table — JPA's JOINED inheritance. A mixed feed is therefore one
query, while each type keeps its own columns instead of a pile of nullable ones.

- **Everything is created as a DRAFT.** Publishing is a separate, deliberate step.
  A draft returns 404 to everyone except an admin, rather than 403, so an
  unpublished slug cannot be probed for existence.
- **Publishing stamps the date only once.** Archiving and re-publishing keeps the
  original date, so the feed does not reshuffle.
- **Nothing is hard-deleted.** Archive withdraws material from the site; comments,
  likes and progress from later phases all reference these rows.
- **Article HTML is sanitised on write** against the OWASP allow-list. What is
  stored is already safe, so no reader depends on the frontend escaping correctly.
  The editor's toolbar is convenience, never a security boundary.
- **Only the YouTube video id is stored.** Watch, `youtu.be`, embed, shorts, live
  and mobile URLs — with playlists or timestamps attached — all collapse to the
  same 11 characters, and the frontend builds a `youtube-nocookie` embed from it.
- **Slugs never change on rename**, because they are already in published URLs.
- **Sorting is restricted to a known list** (`newest`, `oldest`, `popular`,
  `liked`) rather than passed through to JPA, so a client cannot sort by arbitrary
  columns or probe the schema through error messages.

## How the taxonomy works

Topics form a tree (Agronomy > Soil Science > Soil Fertility). Exams are syllabuses
that draw on that shared pool, so **one topic can belong to any number of exams**
without being duplicated — `exam_topics` is a many-to-many join, and removing a
topic from one exam leaves the others untouched.

Three rules the server enforces, because none of them can be left to the UI:

- A topic cannot be moved beneath its own descendant. The service walks the
  ancestor chain first; a cycle would make the tree builder recurse forever.
- Deleting a topic that still has children is refused with a 409. The foreign key
  would happily cascade and take the whole branch with it, which an admin cannot undo.
- Setting an exam's topics replaces the entire set rather than applying a delta,
  so repeating the call changes nothing.

A syllabus may include a sub-topic without its parent. Those are promoted to roots
when the tree is assembled, rather than being dropped and silently hidden.
