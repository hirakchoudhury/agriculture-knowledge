"""End-to-end check of the phase 4 material endpoints against a running local API.

Requires an ADMIN account (see app.bootstrap-admin-emails in the README).
"""

import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
ADMIN_EMAIL = "browsertest-phase2@example.com"
ADMIN_PASSWORD = "a-long-enough-password"

passed, failed = [], []


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read().decode() or "null"
            return r.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode() or "null"
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def check(label, actual, expected):
    ok = actual == expected
    (passed if ok else failed).append(label)
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {actual!r}, expected {expected!r}")


def check_true(label, actual):
    check(label, bool(actual), True)


# --- setup -------------------------------------------------------------------
status, body = call("POST", "/auth/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
if status != 200:
    raise SystemExit(f"Could not sign in as {ADMIN_EMAIL}: HTTP {status} {body}")
admin = body["accessToken"]

stamp = int(time.time())
_, learner_body = call("POST", "/auth/register", {
    "email": f"reader+{stamp}@example.com",
    "password": "another-long-password",
    "name": "Reader",
})
learner = learner_body["accessToken"]

_, topic = call("POST", "/admin/topics", {"name": f"Soil Chemistry {stamp}", "displayOrder": 0}, token=admin)
_, other_topic = call("POST", "/admin/topics", {"name": f"Entomology {stamp}", "displayOrder": 1}, token=admin)
_, exam = call("POST", "/admin/exams", {"name": f"ICAR {stamp}", "displayOrder": 0}, token=admin)

# --- articles: drafts are private -------------------------------------------
print("\n--- articles and the draft/publish workflow ---")
status, article = call("POST", "/admin/materials/articles", {
    "title": f"Understanding Soil pH {stamp}",
    "summary": "Why pH governs nutrient availability.",
    "difficulty": "BEGINNER",
    "bodyHtml": "<p>Soil pH controls which nutrients a plant can actually take up.</p>",
    "topicIds": [topic["id"]],
    "examIds": [exam["id"]],
}, token=admin)
check("creating an article returns 201", status, 201)
check("a new article starts as a DRAFT", article["status"], "DRAFT")
check("the type discriminator is set", article["type"], "ARTICLE")
check("it is not published, so it has no publish date", article["publishedAt"], None)
check("reading time is at least one minute", article["readingMinutes"] >= 1, True)
check("topics are attached", [t["id"] for t in article["topics"]], [topic["id"]])
check("exams are attached", [e["id"] for e in article["exams"]], [exam["id"]])

status, _ = call("GET", f"/materials/{article['slug']}")
check("an anonymous reader gets 404 for a draft, not 403", status, 404)

status, _ = call("GET", f"/materials/{article['slug']}", token=learner)
check("a signed-in non-admin also gets 404 for a draft", status, 404)

status, seen_by_admin = call("GET", f"/materials/{article['slug']}", token=admin)
check("an admin can open their own draft", status, 200)

status, listing = call("GET", "/materials")
check("drafts never appear in the public list",
      any(m["slug"] == article["slug"] for m in listing["content"]), False)

# --- publishing --------------------------------------------------------------
status, published = call("PATCH", f"/admin/materials/{article['id']}/status",
                         {"status": "PUBLISHED"}, token=admin)
check("publishing returns 200", status, 200)
check("status becomes PUBLISHED", published["status"], "PUBLISHED")
check("publishedAt is now set", published["publishedAt"] is not None, True)

# Read the persisted value rather than the one still in memory: Postgres stores
# timestamps to microseconds, so the freshly-created Instant has more precision
# than anything that has been through the database.
_, after_publish = call("GET", f"/materials/{article['slug']}", token=admin)
first_published_at = after_publish["publishedAt"]

call("PATCH", f"/admin/materials/{article['id']}/status", {"status": "ARCHIVED"}, token=admin)
call("PATCH", f"/admin/materials/{article['id']}/status", {"status": "PUBLISHED"}, token=admin)
_, after_republish = call("GET", f"/materials/{article['slug']}", token=admin)
check("re-publishing keeps the ORIGINAL date, so the feed order is stable",
      after_republish["publishedAt"], first_published_at)

status, detail = call("GET", f"/materials/{article['slug']}")
check("a published article is publicly readable", status, 200)
check("and carries its body", "nutrients" in (detail["bodyHtml"] or ""), True)

# --- view counting -----------------------------------------------------------
before = detail["viewCount"]
call("GET", f"/materials/{article['slug']}")
status, after = call("GET", f"/materials/{article['slug']}")
check("reading a material increments its view count", after["viewCount"] > before, True)

# --- HTML sanitisation -------------------------------------------------------
print("\n--- stored HTML is sanitised ---")
dirty = (
    "<p>Safe text</p>"
    "<script>alert('xss')</script>"
    "<img src=x onerror=alert('xss')>"
    "<a href=\"javascript:alert(1)\">click</a>"
    "<a href=\"https://example.com\">ok</a>"
)
status, hostile = call("POST", "/admin/materials/articles", {
    "title": f"Sanitiser check {stamp}",
    "difficulty": "BEGINNER",
    "bodyHtml": dirty,
    "topicIds": [],
    "examIds": [],
}, token=admin)
stored = hostile["bodyHtml"]
check("the script tag is stripped", "<script" in stored.lower(), False)
check("the inline event handler is stripped", "onerror" in stored.lower(), False)
check("the javascript: URL is stripped", "javascript:" in stored.lower(), False)
check("legitimate text survives", "Safe text" in stored, True)
check("legitimate links survive", "example.com" in stored, True)

# --- YouTube URL parsing -----------------------------------------------------
print("\n--- YouTube link parsing ---")
VIDEO_ID = "dQw4w9WgXcQ"
shapes = {
    "watch URL": f"https://www.youtube.com/watch?v={VIDEO_ID}",
    "watch URL with extra params": f"https://www.youtube.com/watch?v={VIDEO_ID}&t=42s&list=PL1",
    "youtu.be short link": f"https://youtu.be/{VIDEO_ID}",
    "embed URL": f"https://www.youtube.com/embed/{VIDEO_ID}",
    "shorts URL": f"https://www.youtube.com/shorts/{VIDEO_ID}",
    "mobile URL": f"https://m.youtube.com/watch?v={VIDEO_ID}",
    "bare video id": VIDEO_ID,
}
video_ids = []
for index, (label, url) in enumerate(shapes.items()):
    status, video = call("POST", "/admin/materials/videos", {
        "title": f"Video {index} {stamp}",
        "difficulty": "BEGINNER",
        "youtubeUrl": url,
        "topicIds": [other_topic["id"]],
        "examIds": [],
    }, token=admin)
    if status == 201:
        video_ids.append(video["id"])
    check(f"{label} resolves to the bare id", video.get("youtubeId"), VIDEO_ID)

check("a video gets a free YouTube thumbnail by default",
      "ytimg.com" in (video.get("thumbnailUrl") or ""), True)

status, err = call("POST", "/admin/materials/videos", {
    "title": f"Not a video {stamp}",
    "difficulty": "BEGINNER",
    "youtubeUrl": "https://vimeo.com/12345",
    "topicIds": [],
    "examIds": [],
}, token=admin)
check("a non-YouTube link is rejected with 400", status, 400)

# --- filtering ---------------------------------------------------------------
print("\n--- filtering and search ---")
for vid in video_ids:
    call("PATCH", f"/admin/materials/{vid}/status", {"status": "PUBLISHED"}, token=admin)

status, by_type = call("GET", "/materials?type=VIDEO&size=50")
check("filtering by type returns only that type",
      all(m["type"] == "VIDEO" for m in by_type["content"]), True)
check("and finds the videos just published", by_type["totalElements"] >= len(video_ids), True)

status, by_topic = call("GET", f"/materials?topicId={topic['id']}&size=50")
check("filtering by topic finds the tagged article",
      any(m["slug"] == article["slug"] for m in by_topic["content"]), True)
check("and excludes material tagged with a different topic",
      all(m["type"] != "VIDEO" for m in by_topic["content"]), True)

status, by_exam = call("GET", f"/materials?examId={exam['id']}&size=50")
check("filtering by exam works", by_exam["totalElements"] >= 1, True)

status, by_query = call("GET", f"/materials?q=soil%20ph&size=50")
check("free-text search matches the title case-insensitively",
      any(m["slug"] == article["slug"] for m in by_query["content"]), True)

status, no_match = call("GET", "/materials?q=zzzznothingmatchesthis")
check("a search with no matches returns an empty page", no_match["totalElements"], 0)

status, page = call("GET", "/materials?size=2")
check("page size is respected", len(page["content"]) <= 2, True)
check("the envelope reports total pages", page["totalPages"] >= 1, True)

status, capped = call("GET", "/materials?size=5000")
check("an absurd page size is clamped rather than honoured", capped["size"] <= 50, True)

status, summary_page = call("GET", f"/materials?topicId={topic['id']}")
if summary_page["content"]:
    check("list cards carry topic names without an extra request",
          len(summary_page["content"][0]["topicNames"]) >= 1, True)

# --- authorisation -----------------------------------------------------------
print("\n--- authorisation ---")
status, _ = call("POST", "/admin/materials/articles", {
    "title": "Sneaky", "difficulty": "BEGINNER", "bodyHtml": "<p>x</p>",
    "topicIds": [], "examIds": [],
}, token=learner)
check("a normal user cannot create material (403)", status, 403)

status, _ = call("PATCH", f"/admin/materials/{article['id']}/status",
                 {"status": "ARCHIVED"}, token=learner)
check("a normal user cannot publish or archive (403)", status, 403)

status, _ = call("POST", "/admin/materials/articles", {
    "title": "Anon", "difficulty": "BEGINNER", "bodyHtml": "<p>x</p>",
    "topicIds": [], "examIds": [],
})
check("an anonymous caller gets 401", status, 401)

# --- validation and type safety ----------------------------------------------
print("\n--- validation ---")
status, _ = call("POST", "/admin/materials/articles", {
    "title": "", "difficulty": "BEGINNER", "bodyHtml": "<p>x</p>",
    "topicIds": [], "examIds": [],
}, token=admin)
check("a blank title returns 400", status, 400)

status, _ = call("POST", "/admin/materials/articles", {
    "title": "No body", "difficulty": "BEGINNER", "bodyHtml": "",
    "topicIds": [], "examIds": [],
}, token=admin)
check("an empty body returns 400", status, 400)

status, _ = call("POST", "/admin/materials/articles", {
    "title": "Bad topic", "difficulty": "BEGINNER", "bodyHtml": "<p>x</p>",
    "topicIds": [999999], "examIds": [],
}, token=admin)
check("an unknown topic id returns 404", status, 404)

status, _ = call("PUT", f"/admin/materials/videos/{article['id']}", {
    "title": "Wrong type", "difficulty": "BEGINNER",
    "youtubeUrl": f"https://youtu.be/{VIDEO_ID}", "topicIds": [], "examIds": [],
}, token=admin)
check("updating an article through the video endpoint is refused (400)", status, 400)

# --- archiving ---------------------------------------------------------------
print("\n--- archiving ---")
status, _ = call("DELETE", f"/admin/materials/{article['id']}", token=admin)
check("archiving returns 204", status, 204)

status, _ = call("GET", f"/materials/{article['slug']}")
check("archived material disappears from the public site (404)", status, 404)

status, admin_view = call("GET", "/admin/materials?status=ARCHIVED&size=50", token=admin)
check("but an admin can still list it",
      any(m["slug"] == article["slug"] for m in admin_view["content"]), True)

print(f"\n{'=' * 60}")
# --- cleanup -----------------------------------------------------------------
# Archive everything this run created. There is no hard delete by design, but
# leaving test material PUBLISHED would put it on the real public library page.
print("--- cleanup ---")
archived = 0
for page_number in range(0, 10):
    _, admin_page = call("GET", f"/admin/materials?size=50&page={page_number}", token=admin)
    rows = admin_page.get("content", []) if admin_page else []
    if not rows:
        break
    for row in rows:
        if str(stamp) in row["title"] and row["status"] != "ARCHIVED":
            call("DELETE", f"/admin/materials/{row['id']}", token=admin)
            archived += 1
print(f"archived {archived} item(s) created by this run")

print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all material checks passed")
