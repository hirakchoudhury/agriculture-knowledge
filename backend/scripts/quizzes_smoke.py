"""End-to-end check of the phase 6 quiz endpoints.

Requires an ADMIN account (see app.bootstrap-admin-emails in the README).

The most important assertion in this file is that the answer key never appears in
the take-the-quiz payload. Everything else is a correctness check; that one is a
security check.
"""

import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
ADMIN_EMAIL = "browsertest-phase2@example.com"
ADMIN_PASSWORD = "a-long-enough-password"

passed, failed = [], []


def call(method, path, body=None, token=None, raw=False):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as r:
            text = r.read().decode() or "null"
            return r.status, (text if raw else json.loads(text))
    except urllib.error.HTTPError as e:
        text = e.read().decode() or "null"
        if raw:
            return e.code, text
        try:
            return e.code, json.loads(text)
        except json.JSONDecodeError:
            return e.code, {"raw": text}


def check_num(label, actual, expected):
    """Decimals cross the wire as JSON numbers, so compare numerically.

    Marks are small and exact in binary (quarters and halves), and the client only
    ever displays them -- all the arithmetic happens server-side in BigDecimal --
    so JSON numbers are the right representation here.
    """
    ok = actual is not None and abs(float(actual) - expected) < 1e-9
    (passed if ok else failed).append(label)
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {actual!r}, expected {expected}")


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
    "email": f"quizzer+{stamp}@example.com",
    "password": "another-long-password",
    "name": "Quizzer",
})
learner = learner_body["accessToken"]

_, other_body = call("POST", "/auth/register", {
    "email": f"nosy+{stamp}@example.com",
    "password": "another-long-password",
    "name": "Nosy",
})
nosy = other_body["accessToken"]

# --- creating a quiz ---------------------------------------------------------
print("\n--- authoring ---")
status, quiz = call("POST", "/admin/quizzes", {
    "title": f"Soil fundamentals {stamp}",
    "summary": "Ten minutes on soil basics.",
    "difficulty": "BEGINNER",
    "timeLimitSeconds": 600,
    "passPercentage": 60,
    "shuffleQuestions": False,
    "topicIds": [], "examIds": [],
}, token=admin)
check("creating a quiz returns 201", status, 201)
quiz_id, quiz_slug = quiz["id"], quiz["slug"]
check("a new quiz starts as a DRAFT", quiz["status"], "DRAFT")
check("and has no questions yet", quiz["questions"], [])

questions_payload = {"questions": [
    {
        "text": "Which nutrient is most affected by soil pH?",
        "explanation": "Phosphorus availability drops sharply outside pH 6 to 7.",
        "marks": "2.00", "negativeMarks": "0.50",
        "options": [
            {"text": "Phosphorus", "correct": True},
            {"text": "Carbon", "correct": False},
            {"text": "Silicon", "correct": False},
        ],
    },
    {
        "text": "What does CEC stand for?",
        "explanation": "Cation exchange capacity measures nutrient-holding ability.",
        "marks": "1.00", "negativeMarks": "0.25",
        "options": [
            {"text": "Cation exchange capacity", "correct": True},
            {"text": "Crop establishment coefficient", "correct": False},
        ],
    },
    {
        "text": "Which texture drains fastest?",
        "explanation": "Large particles leave large pores.",
        "marks": "1.00", "negativeMarks": "0.25",
        "options": [
            {"text": "Sand", "correct": True},
            {"text": "Clay", "correct": False},
        ],
    },
]}

status, with_questions = call("PUT", f"/admin/quizzes/{quiz_id}/questions", questions_payload, token=admin)
check("adding questions in bulk returns 200", status, 200)
check("all three questions are stored", len(with_questions["questions"]), 3)
check_num("the total marks are summed", with_questions["totalMarks"], 4.00)
check("options keep their order",
      [o["text"] for o in with_questions["questions"][0]["options"]][0], "Phosphorus")

status, replaced = call("PUT", f"/admin/quizzes/{quiz_id}/questions", questions_payload, token=admin)
check("repeating the call is idempotent, not additive", len(replaced["questions"]), 3)

# --- authoring guardrails ----------------------------------------------------
print("\n--- authoring guardrails ---")
status, err = call("PUT", f"/admin/quizzes/{quiz_id}/questions", {"questions": [{
    "text": "No right answer", "marks": "1.00", "negativeMarks": "0.00",
    "options": [{"text": "A", "correct": False}, {"text": "B", "correct": False}],
}]}, token=admin)
check("a question with no correct option is refused (400)", status, 400)
check("and the message says which question", "Question 1" in err.get("message", ""), True)

status, err = call("PUT", f"/admin/quizzes/{quiz_id}/questions", {"questions": [{
    "text": "Two right answers", "marks": "1.00", "negativeMarks": "0.00",
    "options": [{"text": "A", "correct": True}, {"text": "B", "correct": True}],
}]}, token=admin)
check("a question with two correct options is refused (400)", status, 400)

status, _ = call("PUT", f"/admin/quizzes/{quiz_id}/questions", {"questions": [{
    "text": "Only one option", "marks": "1.00", "negativeMarks": "0.00",
    "options": [{"text": "A", "correct": True}],
}]}, token=admin)
check("a question with fewer than two options is refused (400)", status, 400)

status, still_there = call("GET", f"/admin/quizzes/{quiz_id}", token=admin)
check("a rejected replacement leaves the existing questions intact",
      len(still_there["questions"]), 3)

status, _ = call("POST", "/admin/quizzes", {
    "title": "Sneaky quiz", "difficulty": "BEGINNER", "passPercentage": 50,
    "shuffleQuestions": False, "topicIds": [], "examIds": [],
}, token=learner)
check("a normal user cannot author a quiz (403)", status, 403)

# --- drafts stay private -----------------------------------------------------
print("\n--- drafts ---")
status, _ = call("GET", f"/quizzes/{quiz_slug}")
check("an unpublished quiz is 404 to the public", status, 404)

status, _ = call("POST", f"/quizzes/{quiz_slug}/attempts", token=learner)
check("and cannot be attempted", status, 404)

call("PATCH", f"/admin/materials/{quiz_id}/status", {"status": "PUBLISHED"}, token=admin)

status, summary = call("GET", f"/quizzes/{quiz_slug}")
check("once published the summary is public", status, 200)
check("it reports the question count", summary["questionCount"], 3)
check_num("and the marks on offer", summary["totalMarks"], 4.00)

status, feed = call("GET", f"/materials?type=QUIZ&q=Soil+fundamentals+{stamp}")
check("a quiz appears in the material feed like any other material",
      any(m["slug"] == quiz_slug for m in feed["content"]), True)

# --- THE SECURITY CHECK ------------------------------------------------------
print("\n--- the answer key must not leak ---")
status, attempt_raw = call("POST", f"/quizzes/{quiz_slug}/attempts", token=learner, raw=True)
check("starting an attempt returns 201", status, 201)

lowered = attempt_raw.lower()
check("the take-the-quiz payload contains no 'correct' field",
      "correct" in lowered, False)
check("nor an 'iscorrect' field", "iscorrect" in lowered, False)
check("nor the explanations", "explanation" in lowered, False)
check("nor the phosphorus explanation text", "availability drops" in lowered, False)

attempt = json.loads(attempt_raw)
check("but the questions are there", len(attempt["questions"]), 3)
check("with their options", len(attempt["questions"][0]["options"]), 3)
check("and a server-computed deadline", attempt["expiresAt"] is not None, True)

status, summary_raw = call("GET", f"/quizzes/{quiz_slug}", raw=True)
check("the pre-attempt summary leaks nothing either",
      "correct" in summary_raw.lower(), False)

status, resumed = call("POST", f"/quizzes/{quiz_slug}/attempts", token=learner)
check("starting again resumes the open attempt rather than creating another",
      resumed["attemptId"], attempt["attemptId"])

# --- scoring -----------------------------------------------------------------
print("\n--- scoring ---")
by_text = {q["text"]: q for q in attempt["questions"]}
ph = by_text["Which nutrient is most affected by soil pH?"]
cec = by_text["What does CEC stand for?"]
texture = by_text["Which texture drains fastest?"]

# Right (+2.00), wrong (-0.25), unanswered (0) => 1.75 of 4.00
ph_right = next(o["id"] for o in ph["options"] if o["text"] == "Phosphorus")
cec_wrong = next(o["id"] for o in cec["options"] if o["text"] == "Crop establishment coefficient")

status, result = call("POST", f"/attempts/{attempt['attemptId']}/submit", {"answers": [
    {"questionId": ph["id"], "selectedOptionId": ph_right},
    {"questionId": cec["id"], "selectedOptionId": cec_wrong},
    {"questionId": texture["id"], "selectedOptionId": None},
]}, token=learner)
check("submitting returns 200", status, 200)
check_num("a right answer earns its marks, a wrong one loses the negative marks, "
          "and a blank scores nothing", result["score"], 1.75)
check_num("the total is the marks on offer", result["totalMarks"], 4.00)
check_num("the percentage is computed from those", result["percentage"], 43.8)
check("43.8 percent is below the 60 percent pass mark", result["passed"], False)
check("and it was submitted inside the time limit", result["withinTimeLimit"], True)

review = {q["id"]: q for q in result["questions"]}
check("the right answer is marked right", review[ph["id"]]["answeredCorrectly"], True)
check_num("and awarded its marks", review[ph["id"]]["awarded"], 2.00)
check("the wrong answer is marked wrong", review[cec["id"]]["answeredCorrectly"], False)
check_num("and awarded the negative marks", review[cec["id"]]["awarded"], -0.25)
check_num("the unanswered question is awarded nothing", review[texture["id"]]["awarded"], 0.0)
check("an unanswered question records no selection",
      review[texture["id"]]["selectedOptionId"], None)

check("only after submitting are explanations revealed",
      "availability drops" in (review[ph["id"]]["explanation"] or ""), True)
check("and the correct option is identified",
      review[texture["id"]]["correctOptionId"] is not None, True)

# --- submission guardrails ---------------------------------------------------
print("\n--- submission guardrails ---")
status, _ = call("POST", f"/attempts/{attempt['attemptId']}/submit",
                 {"answers": []}, token=learner)
check("an attempt cannot be submitted twice (409)", status, 409)

status, _ = call("GET", f"/attempts/{attempt['attemptId']}", token=nosy)
check("someone else cannot read your attempt (403)", status, 403)

status, _ = call("GET", f"/attempts/{attempt['attemptId']}")
check("an anonymous caller cannot read an attempt (401)", status, 401)

status, second = call("POST", f"/quizzes/{quiz_slug}/attempts", token=learner)
check("a new attempt can be started after submitting", status, 201)
check("and it is a different attempt", second["attemptId"] != attempt["attemptId"], True)

wrong_question_option = next(o["id"] for o in cec["options"])
status, err = call("POST", f"/attempts/{second['attemptId']}/submit", {"answers": [
    {"questionId": ph["id"], "selectedOptionId": wrong_question_option},
]}, token=learner)
check("an option id from a different question is refused (400)", status, 400)

status, _ = call("POST", f"/attempts/{attempt['attemptId']}/submit",
                 {"answers": []}, token=nosy)
check("you cannot submit someone else's attempt", status in (403, 409), True)

# --- perfect score and history ----------------------------------------------
print("\n--- a perfect score ---")
status, third = call("POST", f"/quizzes/{quiz_slug}/attempts", token=learner)
answers = []
for q in third["questions"]:
    correct_text = {
        "Which nutrient is most affected by soil pH?": "Phosphorus",
        "What does CEC stand for?": "Cation exchange capacity",
        "Which texture drains fastest?": "Sand",
    }[q["text"]]
    answers.append({
        "questionId": q["id"],
        "selectedOptionId": next(o["id"] for o in q["options"] if o["text"] == correct_text),
    })

status, perfect = call("POST", f"/attempts/{third['attemptId']}/submit",
                       {"answers": answers}, token=learner)
check_num("all correct scores full marks", perfect["score"], 4.00)
check_num("which is 100 percent", perfect["percentage"], 100.0)
check("and passes", perfect["passed"], True)

status, history = call("GET", "/users/me/attempts", token=learner)
check("the history lists submitted attempts", history["totalElements"] >= 2, True)
check("newest first", history["content"][0]["attemptId"], third["attemptId"])
check_num("with the score preserved", history["content"][0]["score"], 4.00)

status, empty = call("GET", "/users/me/attempts", token=nosy)
check("and shows nothing for someone who has attempted nothing",
      empty["totalElements"], 0)

# --- editing a quiz does not rewrite past results ----------------------------
print("\n--- past results are immutable ---")
call("PUT", f"/admin/quizzes/{quiz_id}/questions", {"questions": [{
    "text": "Replaced question", "marks": "10.00", "negativeMarks": "0.00",
    "options": [{"text": "A", "correct": True}, {"text": "B", "correct": False}],
}]}, token=admin)

status, old_history = call("GET", "/users/me/attempts", token=learner)
check_num("an earlier attempt keeps the total it was marked against",
          old_history["content"][0]["totalMarks"], 4.00)
check_num("and its score", old_history["content"][0]["score"], 4.00)

# --- cleanup -----------------------------------------------------------------
print("\n--- cleanup ---")
call("DELETE", f"/admin/materials/{quiz_id}", token=admin)
print("archived the quiz this run created")

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all quiz checks passed")
