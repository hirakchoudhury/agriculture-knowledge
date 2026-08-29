"""End-to-end check of the phase 7 learning path and progress endpoints.

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
            return r.status, json.loads(r.read().decode() or "null")
    except urllib.error.HTTPError as e:
        text = e.read().decode() or "null"
        try:
            return e.code, json.loads(text)
        except json.JSONDecodeError:
            return e.code, {"raw": text}


def check(label, actual, expected):
    ok = actual == expected
    (passed if ok else failed).append(label)
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {actual!r}, expected {expected!r}")


# --- setup -------------------------------------------------------------------
status, body = call("POST", "/auth/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
if status != 200:
    raise SystemExit(f"Could not sign in as {ADMIN_EMAIL}: HTTP {status} {body}")
admin = body["accessToken"]

stamp = int(time.time())
_, learner_body = call("POST", "/auth/register", {
    "email": f"planner+{stamp}@example.com",
    "password": "another-long-password", "name": "Planner",
})
learner = learner_body["accessToken"]

_, other_body = call("POST", "/auth/register", {
    "email": f"stranger+{stamp}@example.com",
    "password": "another-long-password", "name": "Stranger",
})
stranger = other_body["accessToken"]

created_materials = []


def publish_article(title):
    _, article = call("POST", "/admin/materials/articles", {
        "title": f"{title} {stamp}", "difficulty": "BEGINNER",
        "bodyHtml": f"<p>{title}</p>", "topicIds": [], "examIds": [],
    }, token=admin)
    call("PATCH", f"/admin/materials/{article['id']}/status", {"status": "PUBLISHED"}, token=admin)
    created_materials.append(article["id"])
    return article


first = publish_article("Soil sampling")
second = publish_article("Reading a soil report")
third = publish_article("Correcting acidity")

_, draft = call("POST", "/admin/materials/articles", {
    "title": f"Unpublished note {stamp}", "difficulty": "BEGINNER",
    "bodyHtml": "<p>Draft.</p>", "topicIds": [], "examIds": [],
}, token=admin)
created_materials.append(draft["id"])

# --- creating a path ---------------------------------------------------------
print("\n--- creating a path ---")
status, path = call("POST", "/learning-paths",
                    {"title": "Soil testing, start to finish",
                     "description": "The order I want to work through this."}, token=learner)
check("creating a path returns 201", status, 201)
check("a new path is empty", path["itemCount"], 0)
check("and nothing is completed", path["completedCount"], 0)
path_id = path["id"]

status, _ = call("POST", "/learning-paths", {"title": ""}, token=learner)
check("a blank title is rejected (400)", status, 400)

status, _ = call("POST", "/learning-paths", {"title": "Anonymous path"})
check("an anonymous caller cannot create a path (401)", status, 401)

# --- adding items ------------------------------------------------------------
print("\n--- adding items ---")
status, withOne = call("POST", f"/learning-paths/{path_id}/items",
                       {"materialId": first["id"], "note": "Do this first"}, token=learner)
check("adding an item returns 200", status, 200)
check("the path now has one step", withOne["itemCount"], 1)
check("the note is kept", withOne["items"][0]["note"], "Do this first")

call("POST", f"/learning-paths/{path_id}/items", {"materialId": second["id"]}, token=learner)
status, withThree = call("POST", f"/learning-paths/{path_id}/items",
                         {"materialId": third["id"]}, token=learner)
check("three steps in the order they were added",
      [i["materialId"] for i in withThree["items"]],
      [first["id"], second["id"], third["id"]])

status, _ = call("POST", f"/learning-paths/{path_id}/items",
                 {"materialId": first["id"]}, token=learner)
check("the same material cannot be added twice (409)", status, 409)

status, _ = call("POST", f"/learning-paths/{path_id}/items",
                 {"materialId": draft["id"]}, token=learner)
check("an unpublished material cannot be added (400)", status, 400)

status, _ = call("POST", f"/learning-paths/{path_id}/items",
                 {"materialId": 999999}, token=learner)
check("an unknown material returns 404", status, 404)

# --- ownership ---------------------------------------------------------------
print("\n--- ownership ---")
status, _ = call("GET", f"/learning-paths/{path_id}", token=stranger)
check("someone else cannot read your path (403)", status, 403)

status, _ = call("POST", f"/learning-paths/{path_id}/items",
                 {"materialId": second["id"]}, token=stranger)
check("nor add to it (403)", status, 403)

status, _ = call("DELETE", f"/learning-paths/{path_id}", token=stranger)
check("nor delete it (403)", status, 403)

status, theirs = call("GET", "/learning-paths", token=stranger)
check("and it does not appear in their list", theirs, [])

# --- reordering --------------------------------------------------------------
print("\n--- reordering ---")
item_ids = [i["itemId"] for i in withThree["items"]]
reversed_ids = list(reversed(item_ids))

status, reordered = call("PUT", f"/learning-paths/{path_id}/items/order",
                         {"itemIds": reversed_ids}, token=learner)
check("reordering returns 200", status, 200)
check("the new order is applied", [i["itemId"] for i in reordered["items"]], reversed_ids)

status, again = call("PUT", f"/learning-paths/{path_id}/items/order",
                     {"itemIds": reversed_ids}, token=learner)
check("repeating the same order changes nothing",
      [i["itemId"] for i in again["items"]], reversed_ids)

status, _ = call("PUT", f"/learning-paths/{path_id}/items/order",
                 {"itemIds": item_ids[:2]}, token=learner)
check("an order missing an item is refused (400)", status, 400)

status, _ = call("PUT", f"/learning-paths/{path_id}/items/order",
                 {"itemIds": item_ids + [999999]}, token=learner)
check("an order naming an unknown item is refused (400)", status, 400)

status, _ = call("PUT", f"/learning-paths/{path_id}/items/order",
                 {"itemIds": [item_ids[0], item_ids[0], item_ids[1]]}, token=learner)
check("an order with a duplicate is refused (400)", status, 400)

# --- progress ----------------------------------------------------------------
print("\n--- progress ---")
status, initial = call("GET", f"/progress/{first['id']}", token=learner)
check("progress starts as not completed", initial["completed"], False)

status, done = call("PUT", f"/progress/{first['id']}",
                    {"status": "COMPLETED", "lastPositionSeconds": None}, token=learner)
check("marking complete returns 200", status, 200)
check("it reads as completed", done["completed"], True)
check("and records when", done["completedAt"] is not None, True)

# Read the persisted value rather than the one still in memory: Postgres stores
# timestamps to microseconds, so a freshly-created Instant has more precision than
# anything that has been through the database.
_, persisted = call("GET", f"/progress/{first['id']}", token=learner)
first_completed_at = persisted["completedAt"]

call("PUT", f"/progress/{first['id']}",
     {"status": "COMPLETED", "lastPositionSeconds": None}, token=learner)
_, repeat = call("GET", f"/progress/{first['id']}", token=learner)
check("completing twice keeps the original date",
      repeat["completedAt"], first_completed_at)

status, detail = call("GET", f"/learning-paths/{path_id}", token=learner)
check("the path reflects the completed step", detail["completedCount"], 1)
completed_item = next(i for i in detail["items"] if i["materialId"] == first["id"])
check("and marks the right step", completed_item["completed"], True)

status, listing = call("GET", "/learning-paths", token=learner)
check("the path list carries the completed count", listing[0]["completedCount"], 1)
check("and the total", listing[0]["itemCount"], 3)

status, reopened = call("PUT", f"/progress/{first['id']}",
                        {"status": "IN_PROGRESS", "lastPositionSeconds": None}, token=learner)
check("progress can be undone", reopened["completed"], False)
check("and the completion date is cleared", reopened["completedAt"], None)

status, position = call("PUT", f"/progress/{second['id']}",
                        {"status": "IN_PROGRESS", "lastPositionSeconds": 125}, token=learner)
check("a resume position is stored", position["lastPositionSeconds"], 125)

status, _ = call("PUT", f"/progress/{draft['id']}",
                 {"status": "COMPLETED", "lastPositionSeconds": None}, token=learner)
check("progress cannot be recorded against a draft (400)", status, 400)

status, strangers_view = call("GET", f"/progress/{first['id']}", token=stranger)
check("progress is per learner, not shared", strangers_view["completed"], False)

# --- progress is independent of any path -------------------------------------
print("\n--- progress outlives the path ---")
call("PUT", f"/progress/{third['id']}", {"status": "COMPLETED", "lastPositionSeconds": None},
     token=learner)

status, second_path = call("POST", "/learning-paths", {"title": "A different plan"}, token=learner)
call("POST", f"/learning-paths/{second_path['id']}/items",
     {"materialId": third["id"]}, token=learner)
status, other_detail = call("GET", f"/learning-paths/{second_path['id']}", token=learner)
check("material completed elsewhere shows as completed in a new path",
      other_detail["completedCount"], 1)

call("DELETE", f"/learning-paths/{second_path['id']}", token=learner)
status, after_delete = call("GET", f"/progress/{third['id']}", token=learner)
check("deleting a path does not erase progress", after_delete["completed"], True)

# --- removing items and deleting ---------------------------------------------
print("\n--- removing ---")
status, trimmed = call("DELETE", f"/learning-paths/{path_id}/items/{item_ids[0]}", token=learner)
check("removing an item returns 200", status, 200)
check("the path is one shorter", trimmed["itemCount"], 2)

status, _ = call("DELETE", f"/learning-paths/{path_id}/items/999999", token=learner)
check("removing an unknown item returns 404", status, 404)

status, still_there = call("GET", f"/materials/{first['slug']}")
check("removing an item does not touch the material itself", status, 200)

status, _ = call("DELETE", f"/learning-paths/{path_id}", token=learner)
check("deleting a path returns 204", status, 204)

status, _ = call("GET", f"/learning-paths/{path_id}", token=learner)
check("and it is gone (404)", status, 404)

status, empty = call("GET", "/learning-paths", token=learner)
check("the list is empty again", empty, [])

# --- cleanup -----------------------------------------------------------------
print("\n--- cleanup ---")
for material_id in created_materials:
    call("DELETE", f"/admin/materials/{material_id}", token=admin)
print(f"archived {len(created_materials)} fixture(s)")

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all learning path checks passed")
