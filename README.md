# Agriculture Knowledge

A two-role learning platform for agriculture competitive exams. Admins publish
articles, YouTube lessons and MCQ quizzes organised by exam and topic; learners
read, watch, attempt, like, comment and assemble their own learning paths.

**Status: all 8 phases complete.** Skeleton, database, authentication, the
exam/topic taxonomy, articles and video lessons, likes and comments, MCQ quizzes
with server-side marking, learner-built paths with progress tracking, and
full-text search with an admin dashboard.

Not yet deployed. See the deployment section below.

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

## Passwords, verification and reset

**Password rules.** At least 8 characters, with a capital letter, a number and a
symbol - enforced by one `@StrongPassword` annotation used by both registration and
reset, so the two cannot drift apart. The validator checks each rule separately so
the error says *which* rule failed rather than a flat "not strong enough"; the
sign-up form shows the same checklist live as you type.

Worth knowing: composition rules like these are weaker against guessing than a
plain length minimum, and they reliably push people towards predictable
substitutions. Current NIST guidance prefers length. This is implemented as asked
because it is what exam portal users expect, but a longer minimum would be stronger.

**Email verification.** Registration creates the account and emails a 6-digit code;
it deliberately returns **no session**, because an unverified account cannot do
anything. Entering the code verifies and signs them in together, so they are not
asked for the password they typed a minute ago.

- Codes are stored as SHA-256 hashes, never in the clear. A six-digit code is weak
  enough by itself without a leaked table handing over live ones.
- Five wrong guesses burns the code. A million possibilities is brute-forceable in
  seconds without a ceiling.
- Issuing a new code voids the outstanding one, so only ever one is live.
- Codes expire after 15 minutes.
- Signing in unverified returns **403 with a distinguishable message**, so the
  client can offer the code screen rather than claiming the password was wrong. The
  check happens *after* the password matches, or it would reveal which addresses are
  registered.
- Accounts that existed before this feature were marked verified by the migration.
  Locking people out of an account they already use, for a rule that did not exist
  when they signed up, is not defensible.
- Google accounts arrive already verified. Google proved the address.

**Password reset.** Same code mechanism, different purpose - the `purpose` column
means a reset code cannot verify an address and vice versa.

- `forgot-password` returns **204 whatever the address is**. Saying "no such
  account" turns the endpoint into a way to discover who has registered.
- Resetting revokes every refresh token, so no session can be renewed.
- **Known window:** an access token issued before the reset keeps working until it
  expires, up to 15 minutes. That is inherent to stateless JWTs - nothing consults
  the database on a normal request, which is what makes them fast. Closing it would
  mean a database lookup on every authenticated call, and with the API in Amsterdam
  and Neon in Ohio that is ~90ms added to everything. The smoke test asserts this
  behaviour explicitly rather than leaving it implied.
- Resetting also marks the address verified: reaching the inbox proves it as surely
  as the sign-up code does.

## Sending email

Verification and reset codes go out through **Brevo's HTTP API**, not SMTP.

**Railway blocks outbound SMTP.** This is worth stating plainly because it costs
an afternoon to discover:

| Attempt | Result |
| ------- | ------ |
| `smtp.gmail.com:587`, STARTTLS, 5s timeout | connection timeout |
| `smtp.gmail.com:465`, implicit SSL, 20s timeout | connection timeout |
| Same container to Neon in Ohio | fine |
| Same container to the internet generally | fine |

Bad credentials give an authentication error (`535`), not a connection timeout.
Timeouts on two different SMTP ports, while everything else on the network works,
means the platform is blocking the ports — standard on PaaS hosts to stop spam
relay. No Gmail app password can fix it.

An HTTP call to port 443 is indistinguishable from any other API request and goes
straight out, so the transport is an interface with three implementations, chosen
at startup in this order:

1. **Brevo HTTP API**, when `BREVO_API_KEY` is set. The only one that works on
   Railway.
2. **SMTP**, when `spring.mail.host` and username are set. Works locally and on
   hosts that permit it.
3. **Log only**, when neither is. Local development needs no credentials at all.

The startup log always names the transport in use, so the running configuration is
never a guess.

### Configuring Brevo

1. Create an account at https://www.brevo.com — free, no card
2. Verify your sender address at https://app.brevo.com/senders/list. Without this
   the API accepts the request and then declines to deliver.
3. Create a key at https://app.brevo.com/settings/keys/api (account name in the
   top-right, then **SMTP & API**, then **API Keys**)
4. Set these:

```
BREVO_API_KEY=xkeysib-...
MAIL_FROM=your-verified-sender@example.com
```

Free tier is 300 messages a day and delivers to any address without owning a
domain, which is what a public sign-up flow needs.

**When no transport is configured the code is written to the application log
instead of sent.** That keeps local work simple, but in production it means anyone
with log access can take over any account, and sign-up is silently broken for real
users — so the prod profile logs an ERROR about it on startup.

## Theme

Light, dark, or follow the system - three states, not a two-way switch, because a
toggle cannot express "match my device", which is what most people want and the only
setting that keeps up when the phone switches at sunset.

The choice is stored in `localStorage` and applied by a tiny script in `<head>`
before first paint, which is what prevents the flash of the wrong theme on load.
An explicit choice beats the OS setting in both directions.

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
| POST   | `/api/v1/auth/verify-email` | public | Code from the email; signs them in  |
| POST   | `/api/v1/auth/resend-verification` | public | Always 204                   |
| POST   | `/api/v1/auth/forgot-password` | public | Always 204                       |
| POST   | `/api/v1/auth/reset-password` | public | Code plus a new password          |
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
| GET    | `/api/v1/learning-paths` | user   | The caller's own paths            |
| POST   | `/api/v1/learning-paths` | user   | Also PUT and DELETE by id         |
| POST   | `/api/v1/learning-paths/{id}/items` | owner | Append a step          |
| PUT    | `/api/v1/learning-paths/{id}/items/order` | owner | The complete order |
| PUT    | `/api/v1/progress/{materialId}` | user | Mark done, or store a position |
| GET    | `/api/v1/admin/stats`    | admin  | Dashboard counts                  |

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
    path/           Learning paths and per-material progress
    admin/          Dashboard statistics
    health/         HealthController
  src/main/resources/db/migration/   Flyway migrations
  scripts/auth_smoke.py              End-to-end auth check
  scripts/catalog_smoke.py           End-to-end taxonomy check
  scripts/materials_smoke.py         End-to-end article and video check
  scripts/engagement_smoke.py        End-to-end like and comment check
  scripts/quizzes_smoke.py           End-to-end quiz and marking check
  scripts/paths_smoke.py             End-to-end learning path check
  scripts/cleanup_test_data.sql      Removes rows the smoke scripts leave behind
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

## How learning paths work

A path is a learner's own ordering of published material. Everything is private to
its owner; `is_public` reserves sharing without building it.

- **Progress belongs to the learner and the material, not to the path.** An article
  finished inside one path counts as finished everywhere it appears, and deleting a
  path leaves progress untouched. There is a test for both.
- **Reordering sends the complete list of item ids**, not a move instruction, so a
  dropped request cannot leave the order half-applied and repeating it is a no-op.
  An order that does not name exactly the current set is rejected, which catches a
  stale client built before someone added a step.
- **The same material cannot appear twice in one path**, enforced by a unique
  constraint as well as a check, because the progress view would otherwise show it
  as two independent steps.
- **Only published material can be added**, or the path would contain something the
  learner cannot open.
- **Completion is stamped once.** Re-marking something done keeps the original date
  rather than moving it.
- Reordering in the UI uses move-up and move-down buttons rather than drag and
  drop: it works with a keyboard and a screen reader, needs no library, and is
  easier to hit on a phone.

Path access returns **403, not 404**, for someone else's path — a deliberate
difference from unpublished material. A learner knows their own paths exist, so
hiding the distinction buys nothing and a clear message is more useful.

## Search

Free-text search runs against a generated `tsvector` column with a GIN index, so
it stems and ranks rather than substring-matching: "demonstrations" finds
"Demonstration", and "practice sets" finds "Practice Set". Title matches outrank
summary matches.

The column is `GENERATED ALWAYS`, so it can never drift from the text it
summarises — Postgres recomputes it on every write.

The query itself is native, because `@@` and `ts_rank` have no JPQL equivalent.
It returns **ids**, which are then loaded through JPA: mapping a native result
onto a JOINED inheritance hierarchy is fragile, so the exotic SQL stays in one
place and entity loading stays on the well-trodden path.

## Rate limits

Keyed by IP, in-memory, and therefore per-instance — on more than one container
these belong in Redis.

| Endpoint | Default | Override |
| -------- | ------- | -------- |
| Login    | 15 per 5 minutes | `APP_RATE_LIMIT_LOGIN_ATTEMPTS` |
| Register | 25 per hour | `APP_RATE_LIMIT_REGISTRATIONS` |
| Comments | 5 per minute, per account | — |

The auth ceilings are deliberately not tight. This audience is often behind one
shared connection — a college lab, a coaching centre, a mobile carrier NAT — where
a handful of sign-ups an hour is normal traffic rather than an attack. A correct
password clears the login allowance, so someone who mistypes and then succeeds is
not left throttled.

## Deploying

The API runs on Railway from the Dockerfile, the frontend on Vercel, and the
database stays on Neon. Nothing is stored on the API container's disk — Railway's
filesystem is ephemeral and resets on every deploy.

**Order matters.** The API needs to know the frontend's URL for CORS, and the
frontend needs to know the API's URL, so deploy the API first with a placeholder
and come back to it.

### 1. Generate a production JWT secret

Do not reuse the local one.

```bash
openssl rand -base64 32
```

Keep the output to hand; it goes into Railway in the next step and nowhere else.

### 2. The API on Railway

1. New project → **Deploy from GitHub repo** → pick `agriculture-knowledge`.
2. In the service settings, set **Root Directory** to `backend`. Railway then
   finds the Dockerfile and `railway.json` on its own.
3. Add these variables:

| Variable | Value |
| -------- | ----- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://YOUR-NEON-HOST/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | your Neon user |
| `SPRING_DATASOURCE_PASSWORD` | your Neon password |
| `APP_JWT_SECRET` | the secret from step 1 |
| `APP_FRONTEND_URL` | `http://localhost:3000` for now — corrected in step 4 |
| `APP_BOOTSTRAP_ADMIN_EMAILS` | the email you will sign up with |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k` |

   Take the Neon values from the connection string, dropping `channel_binding`
   and prefixing the host with `jdbc:postgresql://`.

   **Use the direct endpoint, not the pooled one.** Neon's connection string
   defaults to a host containing `-pooler`, which is PgBouncer in transaction
   mode. HikariCP already pools connections, so that puts a pooler in front of a
   pooler — and transaction-mode pooling does not support the server-side
   prepared statements the Postgres JDBC driver starts using after a few
   executions, which surfaces later as `prepared statement "S_1" already exists`
   under load. Drop `-pooler` from the host. If you must use the pooled endpoint,
   add `&prepareThreshold=0` to the URL instead. Note this differs from the
   original plan, which assumed Railway's own Postgres and its reference
   variables — you are pointing at Neon instead, so paste the values directly.

   `JAVA_TOOL_OPTIONS` matters more than it looks: without a heap cap the JVM
   sizes itself against the host's memory, not the container's, and gets killed.

4. Settings → Networking → **Generate Domain**. Note the
   `https://something.up.railway.app` address.
5. Check it: `https://your-api.up.railway.app/api/v1/health` should return JSON.
   Flyway runs all eight migrations on first boot.

### 3. The frontend on Vercel

**If this project lives inside OneDrive, deploy with `scripts/deploy-frontend.ps1`
rather than running the CLI directly.** Deploying from a OneDrive path fails with a
bare `fetch failed`: the deployment is created but the upload never completes, so it
sits at `UNKNOWN` with `Builds: . [0ms]`. That was six failures across two days,
both upload modes, with the payload trimmed to 362 bytes. Copying the project to a
path outside OneDrive first worked on the first attempt. The likely cause is
OneDrive's on-demand file hydration interfering with the reads the CLI makes while
uploading.

The script stages the project in `%TEMP%`, checks the `.vercel` link came across so
it cannot accidentally create a second project, and deploys from there.

For the first-time setup:

1. **Add New → Project** → import the same repository.
2. Set **Root Directory** to `frontend`.
3. Add one variable: `NEXT_PUBLIC_API_URL` = your Railway domain, no trailing slash.
4. Deploy, and note the `https://something.vercel.app` address.

### 4. Close the loop

Go back to Railway and set `APP_FRONTEND_URL` to the Vercel URL, then redeploy.
Until you do this, every browser request fails CORS and the site looks broken
while the API is perfectly healthy.

### 5. Make yourself an admin

Sign up on the live site with the address you put in `APP_BOOTSTRAP_ADMIN_EMAILS`,
then restart the Railway service. The log says `Promoted you@example.com to ADMIN`
and an **Admin** link appears in the header.

### 6. Google sign-in (optional)

In the Google Cloud Console, add this authorised redirect URI — it points at the
**API**, not the frontend, which is the single most common mistake:

```
https://your-api.up.railway.app/login/oauth2/code/google
```

Then set `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` and
`..._CLIENT_SECRET` on Railway.

### Things that will bite you

- **Cookies across two domains.** The refresh token is `SameSite=None; Secure`,
  which browsers require when the frontend and API are on different sites. It
  works, but Safari's tracking prevention is unfriendly to it. Once you have a
  custom domain, put the frontend on the apex and the API on `api.`, and the
  cookie becomes same-site.
- **Vercel preview deployments** each get their own URL and will fail CORS. Add
  them to `APP_EXTRA_ALLOWED_ORIGINS` (comma-separated) if you need previews to
  talk to the live API.
- **Railway has no permanent free tier.** After the trial credit it is a paid
  Hobby plan, and a JVM idling at several hundred megabytes uses that allowance
  faster than a Node service would. Check the current terms.
- **Vercel's free plan is non-commercial.** Fine now; relevant if you ever charge
  for this.
- **Connecting GitHub needs a GitHub login on the Vercel account.** If you signed
  up to Vercel with email, `vercel git connect` fails with "You need to add a Login
  Connection to your GitHub account first". Add GitHub under Vercel account
  settings, then connect the repo and set **Root Directory** to `frontend` — after
  which every push deploys itself and the OneDrive problem above stops mattering.
- **Uploads have nowhere to go.** There is no file upload in the app yet, and
  when you add one the files must go to something like Cloudinary, never the
  container's disk.

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
python scripts/paths_smoke.py      # end-to-end learning paths and progress
```

`./mvnw test` runs 48 unit and slice tests with no database or Docker needed. The
six smoke scripts add 270 end-to-end checks against a running API.

The smoke scripts archive the material they create, but they also leave throwaway
accounts, topics and exams behind. `scripts/cleanup_test_data.sql` removes those —
read it before running it, and run it yourself in the Neon SQL editor. There is
deliberately no hard-delete endpoint in the application.

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
