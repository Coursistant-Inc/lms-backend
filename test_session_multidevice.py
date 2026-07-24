#!/usr/bin/env python3
"""Session multi-device integration tests (A/B/C/D)."""

from __future__ import annotations

import re
import sys
import time
from pathlib import Path

import redis
import requests

BASE = "http://localhost:8080/api"
PASSWORD = "Test12345"
ROLE = "USER"
EXPECTED_MAX_AGE = 14 * 24 * 3600  # 1209600

ACCOUNTS = {
    "regtest1": {"email": "regtest1@example.com", "userId": 385},
    "regtest2": {"email": "regtest2@example.com", "userId": 386},
    "regtest3": {"email": "regtest3@example.com", "userId": 387},
    "regtest4": {"email": "regtest4@example.com", "userId": 388},
    "regtest5": {"email": "regtest5@example.com", "userId": 389},
}

results: list[tuple[str, bool, str]] = []


def load_redis_password() -> str:
    env_path = Path(__file__).resolve().parent / ".env"
    for line in env_path.read_text(encoding="utf-8").splitlines():
        if line.startswith("REDIS_DEFAULT_PASSWORD="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError("REDIS_DEFAULT_PASSWORD not found in .env")


def connect_redis(db: int) -> redis.Redis:
    try:
        client = redis.Redis(
            host="localhost",
            port=6379,
            password=load_redis_password(),
            db=db,
            decode_responses=True,
        )
        client.ping()
        return client
    except redis.exceptions.AuthenticationError:
        client = redis.Redis(host="localhost", port=6379, db=db, decode_responses=True)
        client.ping()
        return client


def redis_client() -> redis.Redis:
    return connect_redis(6)


def general_redis() -> redis.Redis:
    return connect_redis(0)


def clear_login_cache(emails: list[str]) -> None:
    r0 = general_redis()
    for email in emails:
        r0.delete(f"user:email:{email}")
        r0.delete(f"user:login:attempts:{email}")
        r0.delete(f"user:login:lock:{email}")


def bust_user_email_cache(email: str) -> None:
    """Allow repeated logins despite password-less Redis user cache."""
    general_redis().delete(f"user:email:{email}")


def record(case_id: str, ok: bool, detail: str = "") -> None:
    results.append((case_id, ok, detail))
    print(f"[{'PASS' if ok else 'FAIL'}] {case_id}: {detail}")


def extract_refresh(resp: requests.Response) -> str | None:
    token = resp.cookies.get("refreshToken")
    if token:
        return token
    try:
        values = resp.raw.headers.getlist("Set-Cookie")
    except Exception:
        sc = resp.headers.get("Set-Cookie")
        values = [sc] if sc else []
    for sc in values:
        m = re.search(r"refreshToken=([^;]+)", sc)
        if m and m.group(1):
            return m.group(1)
    return None


def cookie_max_age(resp: requests.Response) -> int | None:
    try:
        values = resp.raw.headers.getlist("Set-Cookie")
    except Exception:
        sc = resp.headers.get("Set-Cookie")
        values = [sc] if sc else []
    for sc in values:
        m = re.search(r"(?i)Max-Age=(\d+)", sc)
        if m:
            return int(m.group(1))
    return None


def login(email: str, ua: str = "TestDevice/1.0") -> tuple[requests.Response, str | None, str | None]:
    bust_user_email_cache(email)
    resp = requests.post(
        f"{BASE}/v1/auth/login",
        json={"email": email, "password": PASSWORD, "role": ROLE},
        headers={"User-Agent": ua},
        timeout=30,
    )
    access = None
    if resp.ok:
        body = resp.json()
        access = (body.get("data") or {}).get("accessToken")
        bust_user_email_cache(email)
    return resp, extract_refresh(resp), access


def refresh(refresh_token: str) -> requests.Response:
    return requests.post(
        f"{BASE}/v1/auth/refresh-token",
        cookies={"refreshToken": refresh_token},
        timeout=30,
    )


def logout(refresh_token: str, access_token: str) -> requests.Response:
    return requests.post(
        f"{BASE}/v1/auth/logout",
        cookies={"refreshToken": refresh_token},
        headers={"Authorization": f"Bearer {access_token}"},
        timeout=30,
    )


def redis_get(r: redis.Redis, token: str):
    return r.get(f"refresh:token:{token}")


def redis_ttl(r: redis.Redis, token: str) -> int:
    return int(r.ttl(f"refresh:token:{token}"))


def redis_exists(r: redis.Redis, token: str) -> bool:
    return bool(r.exists(f"refresh:token:{token}"))


def run_a(r: redis.Redis) -> None:
    email = ACCOUNTS["regtest1"]["email"]
    resp_a, tok_a, _ = login(email, "DeviceA/1.0")
    resp_b, tok_b, _ = login(email, "DeviceB/1.0")

    ok_a1 = (
        resp_a.status_code == 200
        and resp_b.status_code == 200
        and bool(tok_a)
        and bool(tok_b)
        and tok_a != tok_b
    )
    record(
        "A1",
        ok_a1,
        f"status={resp_a.status_code}/{resp_b.status_code}, tokens_differ={tok_a != tok_b}",
    )

    if not tok_a or not tok_b:
        record("A2", False, "missing tokens from A1")
        record("A3", False, "missing tokens from A1")
    else:
        ra = refresh(tok_a)
        new_a = extract_refresh(ra)
        ok_a2 = ra.status_code == 200 and bool(ra.json().get("data")) and bool(new_a)
        record("A2", ok_a2, f"status={ra.status_code}, rotated={new_a != tok_a}")

        rb = refresh(tok_b)
        new_b = extract_refresh(rb)
        ok_a3 = rb.status_code == 200 and bool(rb.json().get("data")) and bool(new_b)
        record("A3", ok_a3, f"status={rb.status_code}, rotated={new_b != tok_b}")

    email2 = ACCOUNTS["regtest2"]["email"]
    tokens: list[str] = []
    for i in range(6):
        resp, tok, _ = login(email2, f"DeviceLimit/{i}")
        if resp.status_code != 200 or not tok:
            break
        tokens.append(tok)
        time.sleep(0.1)

    if len(tokens) < 6:
        record("A4", False, f"only got {len(tokens)} login tokens")
        return

    oldest = tokens[0]
    newest = tokens[-1]
    old_resp = refresh(oldest)
    new_resp = refresh(newest)
    alive = sum(1 for t in tokens if redis_exists(r, t))
    ok_a4 = (
        old_resp.status_code == 401
        and old_resp.json().get("code") == "REFRESH_TOKEN_INVALID"
        and new_resp.status_code == 200
        and alive <= 5
    )
    record(
        "A4",
        ok_a4,
        f"oldest_status={old_resp.status_code} code={old_resp.json().get('code')}, "
        f"newest_status={new_resp.status_code}, redis_alive_of_6={alive}",
    )


def run_b() -> None:
    email = ACCOUNTS["regtest3"]["email"]
    _, tok_a, access_a = login(email, "LogoutA/1.0")
    _, tok_b, _ = login(email, "LogoutB/1.0")
    if not tok_a or not tok_b or not access_a:
        record("B1", False, "login failed")
        record("B2", False, "skipped")
        record("B3", False, "skipped")
        return

    lo = logout(tok_a, access_a)
    max_age = cookie_max_age(lo)
    ok_b1 = lo.status_code == 200 and max_age == 0
    record("B1", ok_b1, f"status={lo.status_code}, Max-Age={max_age}")

    r_old = refresh(tok_a)
    ok_b2 = r_old.status_code == 401 and r_old.json().get("code") == "REFRESH_TOKEN_INVALID"
    record("B2", ok_b2, f"status={r_old.status_code}, code={r_old.json().get('code')}")

    r_b = refresh(tok_b)
    ok_b3 = r_b.status_code == 200 and bool(r_b.json().get("data"))
    record("B3", ok_b3, f"status={r_b.status_code}")


def run_c() -> None:
    email = ACCOUNTS["regtest4"]["email"]

    _, tok, _ = login(email, "Grace/1.0")
    if not tok:
        record("C2", False, "login failed")
        record("C1", False, "skipped")
    else:
        r1 = refresh(tok)
        new_tok = extract_refresh(r1)
        if r1.status_code != 200 or not new_tok:
            record("C2", False, f"first refresh failed status={r1.status_code}")
        else:
            r2 = refresh(tok)
            new_tok2 = extract_refresh(r2)
            ok_c2 = r2.status_code == 200 and new_tok2 == new_tok and bool(r2.json().get("data"))
            record("C2", ok_c2, f"status={r2.status_code}, same_new={new_tok2 == new_tok}")

        _, tok2, _ = login(email, "ExpireOld/1.0")
        if not tok2:
            record("C1", False, "login failed")
        else:
            r1 = refresh(tok2)
            new_tok = extract_refresh(r1)
            print("  waiting 31s for grace window to expire...")
            time.sleep(31)
            r2 = refresh(tok2)
            ok_c1 = r2.status_code == 401 and r2.json().get("code") == "REFRESH_TOKEN_INVALID"
            record(
                "C1",
                ok_c1,
                f"status={r2.status_code}, code={r2.json().get('code')}, rotated={bool(new_tok)}",
            )

    resp, _, _ = login(email, "CookieAge/1.0")
    age = cookie_max_age(resp)
    ok_c3 = resp.status_code == 200 and age == EXPECTED_MAX_AGE
    record("C3", ok_c3, f"Max-Age={age}, expected={EXPECTED_MAX_AGE}")


def run_d(r: redis.Redis) -> None:
    email = ACCOUNTS["regtest5"]["email"]
    user_id = ACCOUNTS["regtest5"]["userId"]
    _, tok, access = login(email, "RedisCheck/1.0")
    if not tok or not access:
        record("D1", False, "login failed")
        record("D2", False, "skipped")
        record("D3", False, "skipped")
        return

    val = redis_get(r, tok)
    expected = f"{user_id}:USER"
    ok_d1 = val is not None and expected in str(val)
    record("D1", ok_d1, f"value={val!r}, expected={expected}")

    ttl = redis_ttl(r, tok)
    ok_d3 = EXPECTED_MAX_AGE - 120 <= ttl <= EXPECTED_MAX_AGE
    record("D3", ok_d3, f"ttl={ttl}, expected~{EXPECTED_MAX_AGE}")

    lo = logout(tok, access)
    exists = redis_exists(r, tok)
    ok_d2 = lo.status_code == 200 and not exists
    record("D2", ok_d2, f"logout_status={lo.status_code}, exists={exists}")


def main() -> int:
    h = requests.get(f"{BASE}/v1", timeout=10)
    if h.status_code != 200:
        print(f"Server not healthy: {h.status_code}")
        return 2

    emails = [a["email"] for a in ACCOUNTS.values()]
    clear_login_cache(emails)

    r = redis_client()
    r.ping()

    print("=== A: multi-device ===")
    run_a(r)
    print("=== B: single-device logout ===")
    run_b()
    print("=== C: rotation / grace / cookie ===")
    run_c()
    print("=== D: redis ===")
    run_d(r)

    print("\n========== SUMMARY ==========")
    passed = sum(1 for _, ok, _ in results if ok)
    failed = sum(1 for _, ok, _ in results if not ok)
    for case_id, ok, detail in results:
        print(f"  {'PASS' if ok else 'FAIL'}  {case_id}  {detail}")
    print(f"\nTotal: {passed} passed, {failed} failed, {len(results)} cases")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
