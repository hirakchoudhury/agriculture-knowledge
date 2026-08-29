"""End-to-end check of the phase 5 like and comment endpoints.

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


def register(prefix, stamp):
    _, body = call("POST", "/auth/register", {
        "email": f"{prefix}+{stamp}@example.com",
        "password": "another-long-password",
        "name": prefix.capitalize(),
    })
    return body["accessToken"], body["user"]["id"]


# --- setup -------------------------------------------------------------------
status, body = call("POST", "/auth/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
if status != 200:
    raise SystemExit(f"Could not sign in as {ADMIN_EMAIL}: HTTP {status} {body}")
admin = body["accessToken"]

stamp = int(time.time())
alice, alice_id = register("alice", stamp)
bob, bob_id = register("bob", stamp)

_, article = call("POST", "/admin/materials/articles", {
    "title": f"Engagement fixture {stamp}",
    "difficulty": "BEGINNER",
    "bodyHtml": "<p>Something worth discussing.</p>",
    "topicIds": [], "examIds": [],
}, token=admin)
material_id = article["id"]
slug = article["slug"]

_, draft = call("POST", "/admin/materials/articles", {
    "title": f"Still a draft {stamp}",
    "difficulty": "BEGINNER",
    "bodyHtml": "<p>Unpublished.</p>",
    "topicIds": [], "examIds": [],
}, token=admin)

call("PATCH", f"/admin/materials/{material_id}/status", {"status": "PUBLISHED"}, token=admin)

# --- likes -------------------------------------------------------------------
print("\n--- likes ---")
status, liked = call("POST", f"/materials/{material_id}/like", token=alice)
check("liking returns 200", status, 200)
check("the response says it is liked", liked["liked"], True)
check("the count went up", liked["likeCount"], 1)

status, again = call("POST", f"/materials/{material_id}/like", token=alice)
check("liking twice is idempotent, not an error", status, 200)
check("and does not double-count", again["likeCount"], 1)

status, detail = call("GET", f"/materials/{slug}", token=alice)
check("the liker sees likedByMe true", detail["likedByMe"], True)
check("the stored counter matches", detail["likeCount"], 1)

status, seen_by_bob = call("GET", f"/materials/{slug}", token=bob)
check("someone else sees likedByMe false", seen_by_bob["likedByMe"], False)

status, anon = call("GET", f"/materials/{slug}")
check("an anonymous reader sees likedByMe false", anon["likedByMe"], False)
check("but still sees the count", anon["likeCount"], 1)

status, bob_liked = call("POST", f"/materials/{material_id}/like", token=bob)
check("a second person can like the same material", bob_liked["likeCount"], 2)

status, listing = call("GET", f"/materials?q=Engagement+fixture+{stamp}", token=alice)
card = next((m for m in listing["content"] if m["id"] == material_id), None)
check("list cards carry likedByMe for the viewer", card["likedByMe"], True)
check("and the like count", card["likeCount"], 2)

status, unliked = call("DELETE", f"/materials/{material_id}/like", token=alice)
check("unliking returns 200", status, 200)
check("the count went back down", unliked["likeCount"], 1)

status, again = call("DELETE", f"/materials/{material_id}/like", token=alice)
check("unliking twice is idempotent", status, 200)
check("and cannot push the count negative", again["likeCount"] >= 0, True)

status, _ = call("POST", f"/materials/{material_id}/like")
check("an anonymous caller cannot like (401)", status, 401)

status, _ = call("POST", f"/materials/{draft['id']}/like", token=alice)
check("an unpublished material cannot be liked (400)", status, 400)

# --- comments ----------------------------------------------------------------
print("\n--- comments ---")
status, root = call("POST", f"/materials/{material_id}/comments",
                    {"body": "Does pH matter more than organic carbon here?"}, token=alice)
check("commenting returns 201", status, 201)
check("the author is recorded", root["authorId"], alice_id)
check("it is marked as the caller's own", root["mine"], True)
check("a new comment has no replies", root["replies"], [])

status, reply = call("POST", f"/materials/{material_id}/comments",
                     {"body": "Both, but pH gates availability.", "parentId": root["id"]}, token=bob)
check("replying returns 201", status, 201)
check("the reply records its parent", reply["parentId"], root["id"])

status, nested = call("POST", f"/materials/{material_id}/comments",
                      {"body": "Trying to nest deeper.", "parentId": reply["id"]}, token=alice)
check("replying to a reply is refused (400)", status, 400)

status, thread = call("GET", f"/materials/{material_id}/comments", token=alice)
check("the thread lists top-level comments", thread["totalElements"], 1)
check("with replies nested underneath", len(thread["content"][0]["replies"]), 1)
check("the viewer's own comment is flagged", thread["content"][0]["mine"], True)
check("someone else's reply is not", thread["content"][0]["replies"][0]["mine"], False)

status, anon_thread = call("GET", f"/materials/{material_id}/comments")
check("comments are readable signed out", status, 200)
check("and nothing is flagged as the anonymous reader's own",
      anon_thread["content"][0]["mine"], False)

status, counted = call("GET", f"/materials/{slug}")
check("the material's comment count includes the reply", counted["commentCount"], 2)

# --- editing -----------------------------------------------------------------
print("\n--- editing ---")
status, edited = call("PATCH", f"/comments/{root['id']}",
                      {"body": "Does pH matter more than organic carbon?"}, token=alice)
check("the author can edit their own comment", status, 200)
check("the new text is stored", "organic carbon?" in edited["body"], True)
check("and it is marked as edited", edited["editedAt"] is not None, True)

status, _ = call("PATCH", f"/comments/{root['id']}", {"body": "Hijacked."}, token=bob)
check("someone else cannot edit it (403)", status, 403)

status, _ = call("PATCH", f"/comments/{root['id']}", {"body": "Admin rewrite."}, token=admin)
check("not even an admin can rewrite someone's words (403)", status, 403)

status, _ = call("POST", f"/materials/{material_id}/comments", {"body": "   "}, token=alice)
check("a blank comment is rejected (400)", status, 400)

# --- deleting ----------------------------------------------------------------
print("\n--- deleting ---")
status, _ = call("DELETE", f"/comments/{reply['id']}", token=admin)
check("an admin can remove any comment", status, 204)

status, after_delete = call("GET", f"/materials/{material_id}/comments", token=alice)
check("the deleted reply no longer shows its text",
      any(r["body"] == "[deleted]" for r in after_delete["content"][0]["replies"])
      or len(after_delete["content"][0]["replies"]) == 0, True)

status, recount = call("GET", f"/materials/{slug}")
check("the comment count went down", recount["commentCount"], 1)

# A root with replies must survive deletion, or the thread loses its anchor.
_, second_root = call("POST", f"/materials/{material_id}/comments",
                      {"body": "Second thread."}, token=alice)
_, its_reply = call("POST", f"/materials/{material_id}/comments",
                    {"body": "Reply to the second.", "parentId": second_root["id"]}, token=bob)
call("DELETE", f"/comments/{second_root['id']}", token=alice)

status, thread_after = call("GET", f"/materials/{material_id}/comments")
kept = next((c for c in thread_after["content"] if c["id"] == second_root["id"]), None)
check("a deleted comment with replies is kept as a placeholder", kept is not None, True)
if kept:
    check("its text is replaced", kept["body"], "[deleted]")
    check("its author is no longer named", kept["authorName"], None)
    check("but the reply underneath survives", len(kept["replies"]), 1)

# A root with no replies should disappear entirely rather than leave a stub.
_, lonely = call("POST", f"/materials/{material_id}/comments", {"body": "Nobody replied."}, token=alice)
call("DELETE", f"/comments/{lonely['id']}", token=alice)
status, thread_after2 = call("GET", f"/materials/{material_id}/comments")
check("a deleted comment with no replies is hidden entirely",
      any(c["id"] == lonely["id"] for c in thread_after2["content"]), False)

status, _ = call("DELETE", f"/comments/{root['id']}", token=bob)
check("someone else cannot delete your comment (403)", status, 403)

# --- rate limiting -----------------------------------------------------------
print("\n--- rate limiting ---")
carol, _ = register("carol", stamp)
codes = []
for index in range(8):
    status, _ = call("POST", f"/materials/{material_id}/comments",
                     {"body": f"Rapid comment {index}"}, token=carol)
    codes.append(status)
check("the first few comments are accepted", codes[0], 201)
check("a burst is eventually rate-limited with 429", 429 in codes, True)
check("and the limit is not so tight it blocks normal use",
      codes.count(201) >= 5, True)

# --- cleanup -----------------------------------------------------------------
print("\n--- cleanup ---")
for item_id in (material_id, draft["id"]):
    call("DELETE", f"/admin/materials/{item_id}", token=admin)
print("archived the fixtures this run created")

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all engagement checks passed")
