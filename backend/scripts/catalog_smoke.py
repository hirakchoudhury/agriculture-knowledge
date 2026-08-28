"""End-to-end check of the phase 3 taxonomy endpoints against a running local API.

Requires an ADMIN account. Set app.bootstrap-admin-emails to the address below (or
edit ADMIN_EMAIL) and restart the API so the promotion runs.
"""

import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
ADMIN_EMAIL = "browsertest-phase2@example.com"
ADMIN_PASSWORD = "a-long-enough-password"

passed, failed = [], []
created_topics, created_exams = [], []


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


def find_node(nodes, slug):
    """Depth-first search of a topic tree."""
    for node in nodes:
        if node["slug"] == slug:
            return node
        found = find_node(node["children"], slug)
        if found:
            return found
    return None


# --- sign in -----------------------------------------------------------------
status, body = call("POST", "/auth/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
if status != 200:
    raise SystemExit(f"Could not sign in as {ADMIN_EMAIL}: HTTP {status} {body}")
admin = body["accessToken"]
check("bootstrapped account really is an ADMIN", body["user"]["role"], "ADMIN")

stamp = int(time.time())
learner_email = f"learner+{stamp}@example.com"
status, body = call("POST", "/auth/register",
                    {"email": learner_email, "password": "another-long-password", "name": "Learner"})
learner = body["accessToken"]

# --- topic tree --------------------------------------------------------------
print("\n--- building a topic tree ---")
status, agronomy = call("POST", "/topics".replace("/topics", "/admin/topics"),
                        {"name": f"Agronomy {stamp}", "displayOrder": 0}, token=admin)
check("create a root topic returns 201", status, 201)
created_topics.append(agronomy["id"])
check("slug is derived from the name", agronomy["slug"], f"agronomy-{stamp}")
check("a new root topic has no parent", agronomy["parentId"], None)

status, soil = call("POST", "/admin/topics",
                    {"name": f"Soil Science {stamp}", "parentId": agronomy["id"], "displayOrder": 0},
                    token=admin)
check("create a child topic returns 201", status, 201)
created_topics.append(soil["id"])
check("the child records its parent", soil["parentId"], agronomy["id"])

status, fertility = call("POST", "/admin/topics",
                         {"name": f"Soil Fertility {stamp}", "parentId": soil["id"], "displayOrder": 0},
                         token=admin)
created_topics.append(fertility["id"])
check("a third level is allowed", status, 201)

status, dup = call("POST", "/admin/topics", {"name": f"Agronomy {stamp}", "displayOrder": 9}, token=admin)
created_topics.append(dup["id"])
check("a duplicate name gets a suffixed slug", dup["slug"], f"agronomy-{stamp}-2")

# --- exams -------------------------------------------------------------------
print("\n--- exams ---")
status, icar = call("POST", "/admin/exams",
                    {"name": f"ICAR JRF {stamp}", "description": "Agricultural research fellowship",
                     "displayOrder": 0}, token=admin)
check("create an exam returns 201", status, 201)
created_exams.append(icar["id"])

status, ibps = call("POST", "/admin/exams",
                    {"name": f"IBPS AFO {stamp}", "description": "Agriculture field officer",
                     "displayOrder": 1}, token=admin)
created_exams.append(ibps["id"])
check("a second exam is created", status, 201)

# --- the same topic in two exams ---------------------------------------------
print("\n--- one topic, several exams ---")
status, detail = call("PUT", f"/admin/exams/{icar['id']}/topics",
                      {"topicIds": [agronomy["id"], soil["id"], fertility["id"]]}, token=admin)
check("assigning topics to an exam returns 200", status, 200)

status, detail2 = call("PUT", f"/admin/exams/{ibps['id']}/topics",
                       {"topicIds": [soil["id"]]}, token=admin)
check("the SAME topic can be assigned to a second exam", status, 200)

status, icar_topics = call("GET", f"/exams/{icar['slug']}/topics")
check("first exam still has its topics", bool(find_node(icar_topics, soil["slug"])), True)

status, ibps_topics = call("GET", f"/exams/{ibps['slug']}/topics")
check("second exam has the shared topic too", bool(find_node(ibps_topics, soil["slug"])), True)

check("the shared topic is one row, not a copy",
      find_node(icar_topics, soil["slug"])["id"], find_node(ibps_topics, soil["slug"])["id"])

check("a sub-topic whose parent is not in the syllabus is promoted to a root",
      ibps_topics[0]["slug"], soil["slug"])

# --- tree shape --------------------------------------------------------------
print("\n--- tree shape ---")
status, tree = call("GET", "/topics")
root = find_node(tree, agronomy["slug"])
check("the full tree nests children under parents", root["children"][0]["slug"], soil["slug"])
check("and grandchildren under children",
      root["children"][0]["children"][0]["slug"], fertility["slug"])

status, exams_list = call("GET", "/exams")
mine = next(e for e in exams_list if e["id"] == icar["id"])
check("the exam list carries a topic count", mine["topicCount"], 3)

# --- authorisation -----------------------------------------------------------
print("\n--- authorisation ---")
status, _ = call("POST", "/admin/exams", {"name": "Sneaky", "displayOrder": 0}, token=learner)
check("a normal user cannot create an exam (403)", status, 403)

status, _ = call("DELETE", f"/admin/topics/{fertility['id']}", token=learner)
check("a normal user cannot delete a topic (403)", status, 403)

status, _ = call("POST", "/admin/exams", {"name": "Sneaky", "displayOrder": 0})
check("an anonymous caller gets 401, not 403", status, 401)

status, _ = call("GET", "/exams")
check("but anyone may read the catalogue", status, 200)

# --- integrity rules ---------------------------------------------------------
print("\n--- integrity rules ---")
status, err = call("PUT", f"/admin/topics/{agronomy['id']}",
                   {"name": "Agronomy", "parentId": fertility["id"], "displayOrder": 0}, token=admin)
check("moving a topic beneath its own descendant is rejected (409)", status, 409)

status, err = call("PUT", f"/admin/topics/{agronomy['id']}",
                   {"name": "Agronomy", "parentId": agronomy["id"], "displayOrder": 0}, token=admin)
check("making a topic its own parent is rejected (409)", status, 409)

status, err = call("DELETE", f"/admin/topics/{agronomy['id']}", token=admin)
check("deleting a topic with children is refused (409)", status, 409)
check("and the refusal explains what to do",
      "sub-topics" in err.get("message", ""), True)

status, _ = call("POST", "/admin/topics", {"name": "Orphan", "parentId": 999999, "displayOrder": 0},
                 token=admin)
check("an unknown parent id returns 404", status, 404)

status, _ = call("PUT", f"/admin/exams/{icar['id']}/topics", {"topicIds": [999999]}, token=admin)
check("assigning an unknown topic returns 404", status, 404)

status, _ = call("POST", "/admin/exams", {"name": "", "displayOrder": 0}, token=admin)
check("a blank exam name returns 400", status, 400)

# --- deleting an exam leaves topics alone ------------------------------------
print("\n--- deletion ---")
status, _ = call("DELETE", f"/admin/exams/{ibps['id']}", token=admin)
check("deleting an exam returns 204", status, 204)
created_exams.remove(ibps["id"])

status, tree_after = call("GET", "/topics")
check("deleting an exam does NOT delete its shared topics",
      bool(find_node(tree_after, soil["slug"])), True)

status, _ = call("GET", f"/exams/{ibps['slug']}")
check("the deleted exam is gone (404)", status, 404)

# --- cleanup -----------------------------------------------------------------
for exam_id in created_exams:
    call("DELETE", f"/admin/exams/{exam_id}", token=admin)
for topic_id in reversed(created_topics):  # leaves first, parents last
    call("DELETE", f"/admin/topics/{topic_id}", token=admin)

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all catalogue checks passed")
