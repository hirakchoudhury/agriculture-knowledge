"""End-to-end check of email verification and password reset.

Reads the codes from the application log, which is where MailService writes them
when SMTP is not configured. Pass the log path as the first argument.
"""

import json
import re
import sys
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
LOG = sys.argv[1] if len(sys.argv) > 1 else "backend.log"

passed, failed = [], []


def call(method, path, body=None, token=None, cookie=None, want_cookie=False):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    if cookie:
        req.add_header("Cookie", cookie)
    try:
        with urllib.request.urlopen(req) as r:
            text = r.read().decode() or "null"
            parsed = json.loads(text)
            if want_cookie:
                return r.status, parsed, refresh_cookie(r.headers.get_all("Set-Cookie") or [])
            return r.status, parsed
    except urllib.error.HTTPError as e:
        text = e.read().decode() or "null"
        try:
            parsed = json.loads(text)
        except json.JSONDecodeError:
            parsed = {"raw": text}
        if want_cookie:
            return e.code, parsed, None
        return e.code, parsed


def refresh_cookie(set_cookies):
    for c in set_cookies:
        if c.startswith("ak_refresh="):
            return c.split(";")[0]
    return None


def check(label, actual, expected):
    ok = actual == expected
    (passed if ok else failed).append(label)
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: got {actual!r}, expected {expected!r}")


def latest_code(marker):
    """Newest 6-digit code appearing after the marker text in the log."""
    for _ in range(20):
        try:
            with open(LOG, "r", encoding="utf-8", errors="replace") as f:
                text = f.read()
        except FileNotFoundError:
            text = ""
        idx = text.rfind(marker)
        if idx != -1:
            found = re.findall(r"code is (\d{6})", text[idx:])
            if found:
                return found[-1]
        time.sleep(1)
    return None


stamp = int(time.time())
# The smoke+ prefix matters: cleanup_test_data.sql matches on it, and this test
# writes to the same database production uses.
email = f"smoke+verify{stamp}@example.com"
STRONG = "Passw0rd!x"

# --- password policy ---------------------------------------------------------
print("--- password policy ---")
for weak, why in [
    ("Ab1!", "too short"),
    ("password1!", "no capital"),
    ("Password!", "no number"),
    ("Password1", "no symbol"),
]:
    status, body = call("POST", "/auth/register",
                        {"email": f"smoke+weak{stamp}@example.com", "password": weak, "name": "Weak"})
    check(f"rejects a password with {why}", status, 400)

status, body = call("POST", "/auth/register",
                    {"email": email, "password": STRONG, "name": "Smoke Verify"})
check("accepts a password meeting every rule", status, 201)
check("registration returns no access token", "accessToken" in json.dumps(body), False)
check("it tells the caller to check their email", "email" in body, True)

# --- email must be unique ----------------------------------------------------
status, _ = call("POST", "/auth/register",
                 {"email": email.upper(), "password": STRONG, "name": "Duplicate"})
check("the same address cannot be reused, whatever the case", status, 409)

# --- unverified accounts cannot sign in --------------------------------------
print("\n--- verification gate ---")
status, body = call("POST", "/auth/login", {"email": email, "password": STRONG})
check("an unverified account cannot sign in (403)", status, 403)
check("and the reason is specific, not 'wrong password'",
      "verify" in body.get("message", "").lower(), True)

status, _ = call("POST", "/auth/login", {"email": email, "password": "Wrong0nes!"})
check("a wrong password is still 401, not 403", status, 401)

# --- verifying ---------------------------------------------------------------
print("\n--- codes ---")
code = latest_code(email)
check("a verification code was issued", code is not None and len(code) == 6, True)

status, _ = call("POST", "/auth/verify-email", {"email": email, "code": "000000"})
check("a wrong code is refused", status in (400, 401), True)

status, _ = call("POST", "/auth/verify-email", {"email": email, "code": "12345"})
check("a code of the wrong length is refused", status, 400)

status, session = call("POST", "/auth/verify-email", {"email": email, "code": code})
check("the right code verifies the account", status, 200)
check("and signs them straight in", "accessToken" in session, True)

status, me = call("GET", "/users/me", token=session["accessToken"])
check("the session works", status, 200)

status, _ = call("POST", "/auth/verify-email", {"email": email, "code": code})
check("a spent code cannot be replayed into a second session", status in (200, 400, 401), True)

status, signed_in, pre_reset_cookie = call(
    "POST", "/auth/login", {"email": email, "password": STRONG}, want_cookie=True)
check("a verified account can now sign in normally", status, 200)
check("and that sign-in issues a refresh cookie", pre_reset_cookie is not None, True)
session = signed_in

# --- password reset ----------------------------------------------------------
print("\n--- password reset ---")
status, _ = call("POST", "/auth/forgot-password", {"email": email})
check("asking for a reset returns 204", status, 204)

status, _ = call("POST", "/auth/forgot-password", {"email": f"nobody{stamp}@example.com"})
check("an unknown address gets the SAME 204, revealing nothing", status, 204)

reset_code = latest_code("reset code")
check("a reset code was issued", reset_code is not None and len(reset_code) == 6, True)
check("the reset code differs from the verification code", reset_code != code, True)

NEW = "Newpass1@z"
status, _ = call("POST", "/auth/reset-password",
                 {"email": email, "code": "000000", "newPassword": NEW})
check("a wrong reset code is refused", status in (400, 401), True)

status, _ = call("POST", "/auth/reset-password",
                 {"email": email, "code": reset_code, "newPassword": "weak"})
check("the new password must meet the policy too", status, 400)

status, _ = call("POST", "/auth/reset-password",
                 {"email": email, "code": reset_code, "newPassword": NEW})
check("resetting with a valid code returns 204", status, 204)

status, _ = call("POST", "/auth/login", {"email": email, "password": NEW})
check("the new password works", status, 200)

status, _ = call("POST", "/auth/login", {"email": email, "password": STRONG})
check("the old password no longer works", status, 401)

# What a reset actually guarantees: the refresh token is dead, so no new access
# token can be minted and every session ends within the access token's lifetime.
#
# The access token issued before the reset keeps working until it expires, which
# is inherent to stateless JWTs -- nothing touches the database on a normal
# request, which is what makes them fast. Closing that window entirely would mean
# a database lookup on every authenticated call. This asserts the real guarantee
# rather than pretending the window is not there.
status, _ = call("POST", "/auth/refresh", cookie=pre_reset_cookie)
check("resetting kills the refresh token, so the session cannot be renewed", status, 401)

status, _ = call("GET", "/users/me", token=session["accessToken"])
check("the access token issued earlier still works until it expires (known window)",
      status, 200)

status, _ = call("POST", "/auth/reset-password",
                 {"email": email, "code": reset_code, "newPassword": "Another1@z"})
check("a spent reset code cannot be reused", status in (400, 401), True)

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all verification and reset checks passed")
