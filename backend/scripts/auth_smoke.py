"""End-to-end check of the phase 2 auth endpoints against a running local API."""

import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
EMAIL = f"smoke+{int(time.time())}@example.com"
PASSWORD = "correct-horse-battery"

passed, failed = [], []


def call(method, path, body=None, token=None, cookie=None):
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
            raw = r.read().decode() or "{}"
            return r.status, json.loads(raw), r.headers.get_all("Set-Cookie") or []
    except urllib.error.HTTPError as e:
        raw = e.read().decode() or "{}"
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return e.code, parsed, e.headers.get_all("Set-Cookie") or []


def check(label, actual, expected):
    (passed if actual == expected else failed).append(label)
    mark = "PASS" if actual == expected else "FAIL"
    print(f"[{mark}] {label}: got {actual}, expected {expected}")


def refresh_cookie(set_cookies):
    for c in set_cookies:
        if c.startswith("ak_refresh="):
            return c.split(";")[0]
    return None


print(f"--- registering {EMAIL} ---")
status, body, cookies = call("POST", "/auth/register",
                             {"email": EMAIL, "password": PASSWORD, "name": "Smoke Test"})
check("register returns 201", status, 201)
access = body.get("accessToken")
cookie1 = refresh_cookie(cookies)
check("register issues an access token", bool(access), True)
check("register sets a refresh cookie", bool(cookie1), True)
check("register does NOT leak the refresh token in the body",
      "refreshToken" in json.dumps(body), False)
check("register does NOT leak a password hash", "passwordHash" in json.dumps(body), False)
check("new account defaults to USER role", body.get("user", {}).get("role"), "USER")
check("email stored lower-cased", body.get("user", {}).get("email"), EMAIL.lower())

print("\n--- identity ---")
status, me, _ = call("GET", "/users/me", token=access)
check("/users/me with a token returns 200", status, 200)
check("/users/me returns the right account", me.get("email"), EMAIL.lower())

status, _, _ = call("GET", "/users/me")
check("/users/me without a token returns 401", status, 401)

status, _, _ = call("GET", "/users/me", token="not-a-real-token")
check("/users/me with a junk token returns 401", status, 401)

print("\n--- refresh rotation ---")
status, body2, cookies2 = call("POST", "/auth/refresh", cookie=cookie1)
check("refresh with a valid cookie returns 200", status, 200)
cookie2 = refresh_cookie(cookies2)
check("refresh issues a NEW refresh cookie", cookie2 != cookie1 and bool(cookie2), True)
check("refresh issues a working access token",
      call("GET", "/users/me", token=body2.get("accessToken"))[0], 200)

status, _, _ = call("POST", "/auth/refresh", cookie=cookie1)
check("replaying the OLD refresh cookie is rejected (401)", status, 401)

status, _, _ = call("POST", "/auth/refresh", cookie=cookie2)
check("replay defence revoked the whole session family", status, 401)

print("\n--- login ---")
status, body3, _ = call("POST", "/auth/login", {"email": EMAIL, "password": PASSWORD})
check("login with correct credentials returns 200", status, 200)
check("login token works", call("GET", "/users/me", token=body3.get("accessToken"))[0], 200)

status, err, _ = call("POST", "/auth/login", {"email": EMAIL, "password": "wrong-password"})
check("login with a wrong password returns 401", status, 401)
check("wrong password does not reveal that the account exists",
      err.get("message"), "Email or password is incorrect")

status, err, _ = call("POST", "/auth/login",
                      {"email": "nobody-here@example.com", "password": PASSWORD})
check("unknown email returns the SAME message", err.get("message"),
      "Email or password is incorrect")

print("\n--- validation and conflicts ---")
status, _, _ = call("POST", "/auth/register",
                    {"email": EMAIL.upper(), "password": PASSWORD, "name": "Duplicate"})
check("re-registering the same email (different case) returns 409", status, 409)

status, err, _ = call("POST", "/auth/register",
                      {"email": "short@example.com", "password": "tiny", "name": "Short"})
check("a too-short password returns 400", status, 400)
check("the 400 explains which field failed", "password" in err.get("message", "").lower(), True)

status, _, _ = call("POST", "/auth/register",
                    {"email": "not-an-email", "password": PASSWORD, "name": "Bad"})
check("an invalid email returns 400", status, 400)

print("\n--- logout ---")
status, body4, cookies4 = call("POST", "/auth/login", {"email": EMAIL, "password": PASSWORD})
live_cookie = refresh_cookie(cookies4)
status, _, logout_cookies = call("POST", "/auth/logout", cookie=live_cookie)
check("logout returns 204", status, 204)
check("logout clears the cookie", any("ak_refresh=;" in c or "ak_refresh=\"\"" in c
                                      for c in logout_cookies), True)
status, _, _ = call("POST", "/auth/refresh", cookie=live_cookie)
check("the refresh token is dead after logout", status, 401)

print(f"\n{'=' * 60}")
print(f"passed: {len(passed)}   failed: {len(failed)}")
if failed:
    for f in failed:
        print("  FAILED:", f)
    raise SystemExit(1)
print("all auth checks passed")
