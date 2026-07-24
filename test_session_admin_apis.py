"""
Session + Admin API integration tests (excludes POST /query).
Cases: S1-S12, A1-A6.
"""

import json
import sys
import time
import uuid

import redis
import requests

BASE = "http://localhost:8080/api"
REGTEST_EMAIL = "regtest1@example.com"
REGTEST_PASSWORD = "Test12345"
ADMIN_DEFAULT_PASSWORD = "123"
PASSWORD = "Test12345"

results = []
created_admin_ids = []
created_user_id = None
apitest_email = None


def redis_client(db=0):
    try:
        r = redis.Redis(host="localhost", port=6379, db=db, password="Jerry0415.", decode_responses=True)
        r.ping()
        return r
    except redis.exceptions.AuthenticationError:
        r = redis.Redis(host="localhost", port=6379, db=db, decode_responses=True)
        r.ping()
        return r


def bust_user_cache(email):
    r = redis_client(0)
    r.delete(f"user:email:{email}")
    r.delete(f"user:login:attempts:{email}")
    r.delete(f"user:login:lock:{email}")


def bust_admin_cache(email):
    r = redis_client(0)
    r.delete(f"admin:email:{email}")
    r.delete(f"admin:login:attempts:{email}")
    r.delete(f"admin:login:lock:{email}")


def idem_key():
    return uuid.uuid4().hex


def auth_headers(token, with_idem=False):
    h = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    if with_idem:
        h["Idempotency-Key"] = idem_key()
    return h


def record(case_id, ok, detail):
    status = "PASS" if ok else "FAIL"
    results.append((case_id, status, detail))
    print(f"  [{status}] {case_id}: {detail}")


def wait_healthy(timeout=90):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            r = requests.get(f"{BASE}/v1", timeout=3)
            if r.status_code == 200:
                return True
        except requests.RequestException:
            pass
        time.sleep(2)
    return False


def login_user(email=REGTEST_EMAIL, password=REGTEST_PASSWORD):
    bust_user_cache(email)
    session = requests.Session()
    resp = session.post(f"{BASE}/v1/auth/login", json={
        "email": email,
        "password": password,
        "role": "USER",
    })
    if resp.status_code != 200:
        return None, None, resp
    token = resp.json()["data"]["accessToken"]
    return token, session, resp


def login_admin(email, password=ADMIN_DEFAULT_PASSWORD):
    bust_admin_cache(email)
    session = requests.Session()
    resp = session.post(f"{BASE}/v1/auth/login", json={
        "email": email,
        "password": password,
        "role": "ADMIN",
    })
    if resp.status_code != 200:
        return None, None, resp
    token = resp.json()["data"]["accessToken"]
    return token, session, resp


def get_verification_code(type_, email, retries=10):
    r = redis_client(0)
    key = f"email:verification:{type_}:{email}"
    for _ in range(retries):
        code = r.get(key)
        if code:
            # Jackson JSON serializer stores strings as "\"123456\""
            if isinstance(code, str) and code.startswith('"') and code.endswith('"'):
                code = code[1:-1]
            return code
        time.sleep(0.5)
    return None


def run_s1():
    print("\n=== S1 GET /v1 ===")
    resp = requests.get(f"{BASE}/v1")
    record("S1", resp.status_code == 200, f"{resp.status_code} {resp.text[:120]}")


def extract_refresh_cookie(resp):
    sc = resp.headers.get("Set-Cookie", "")
    if "refreshToken=" in sc:
        return sc.split("refreshToken=")[1].split(";")[0]
    return None


def run_s2_s4():
    print("\n=== S2/S3/S4 login / refresh / logout ===")
    token, session, resp = login_user()
    # Secure cookies are not auto-sent over http:// by requests; pass explicitly.
    old_cookie = extract_refresh_cookie(resp) or session.cookies.get("refreshToken")
    ok = resp.status_code == 200 and token and bool(old_cookie)
    record("S2", ok, f"{resp.status_code} token={'yes' if token else 'no'} cookie={'yes' if old_cookie else 'no'}")
    if not ok:
        record("S3", False, "skipped: login failed")
        record("S4", False, "skipped: login failed")
        return None

    r3 = requests.post(
        f"{BASE}/v1/auth/refresh-token",
        cookies={"refreshToken": old_cookie},
    )
    new_token = None
    try:
        new_token = r3.json().get("data")
    except Exception:
        pass
    new_cookie = extract_refresh_cookie(r3)
    ok3 = r3.status_code == 200 and new_token and new_cookie and new_cookie != old_cookie
    record("S3", ok3, f"{r3.status_code} rotated={new_cookie != old_cookie} body={r3.text[:120]}")
    if not ok3:
        record("S4", False, "skipped: refresh failed")
        return token

    cookie_for_logout = new_cookie or old_cookie
    r4 = requests.post(
        f"{BASE}/v1/auth/logout",
        headers={"Authorization": f"Bearer {new_token}"},
        cookies={"refreshToken": cookie_for_logout},
    )
    ok4 = r4.status_code == 200
    r_fail = requests.post(
        f"{BASE}/v1/auth/refresh-token",
        cookies={"refreshToken": cookie_for_logout},
    )
    fail_ok = r_fail.status_code != 200
    record("S4", ok4 and fail_ok, f"logout={r4.status_code} post_refresh={r_fail.status_code} {r_fail.text[:100]}")
    return new_token or token


def run_s5_s11():
    global apitest_email, created_user_id
    print("\n=== S5-S11 register / password / reset ===")
    ts = int(time.time())
    apitest_email = f"apitest-{ts}@example.com"

    # S5 send register code
    r5 = requests.post(f"{BASE}/v1/auth/email-verifications/register", params={"email": apitest_email})
    record("S5", r5.status_code == 200, f"{r5.status_code} {r5.text[:120]}")
    if r5.status_code != 200:
        for c in ("S6", "S7", "S8", "S9", "S10", "S11"):
            record(c, False, "skipped: send register code failed")
        return

    code = get_verification_code("register", apitest_email)
    if not code:
        record("S6", False, f"Redis miss key=email:verification:register:{apitest_email}")
        for c in ("S7", "S8", "S9", "S10", "S11"):
            record(c, False, "skipped: no verification code")
        return

    # S6 validate
    r6 = requests.post(
        f"{BASE}/v1/auth/email-verifications/register/validate",
        params={"email": apitest_email, "code": code},
        headers={"Idempotency-Key": idem_key()},
    )
    record("S6", r6.status_code == 200, f"{r6.status_code} code={code} {r6.text[:100]}")
    if r6.status_code != 200:
        for c in ("S7", "S8", "S9", "S10", "S11"):
            record(c, False, "skipped: validate failed")
        return

    # S7 register
    r7 = requests.post(f"{BASE}/v1/auth/register", json={
        "email": apitest_email,
        "password": PASSWORD,
        "name": "API Test User",
    })
    ok7 = r7.status_code == 200
    try:
        created_user_id = r7.json().get("data", {}).get("userId")
    except Exception:
        created_user_id = None
    record("S7", ok7, f"{r7.status_code} userId={created_user_id} {r7.text[:120]}")
    if not ok7:
        for c in ("S8", "S9", "S10", "S11"):
            record(c, False, "skipped: register failed")
        return

    # S8 update password to temp then back
    token, session, login_resp = login_user(apitest_email, PASSWORD)
    if not token:
        record("S8", False, f"login before password update failed: {login_resp.status_code} {login_resp.text[:100]}")
        for c in ("S9", "S10", "S11"):
            record(c, False, "skipped")
        return

    temp_pw = "TempPass99"
    r8a = requests.put(
        f"{BASE}/v1/auth/password",
        headers=auth_headers(token, with_idem=True),
        json={
            "email": apitest_email,
            "password": PASSWORD,
            "newPassword": temp_pw,
            "type": "update",
            "role": "USER",
        },
    )
    # re-login with temp and change back
    token2, _, login2 = login_user(apitest_email, temp_pw)
    r8b_ok = False
    if token2:
        r8b = requests.put(
            f"{BASE}/v1/auth/password",
            headers=auth_headers(token2, with_idem=True),
            json={
                "email": apitest_email,
                "password": temp_pw,
                "newPassword": PASSWORD,
                "type": "update",
                "role": "USER",
            },
        )
        r8b_ok = r8b.status_code == 200
        verify_token, _, verify_resp = login_user(apitest_email, PASSWORD)
        record(
            "S8",
            r8a.status_code == 200 and r8b_ok and verify_token,
            f"to_temp={r8a.status_code} back={getattr(r8b, 'status_code', 'n/a')} relogin={'ok' if verify_token else verify_resp.status_code}",
        )
    else:
        record("S8", False, f"temp login failed after change: {login2.status_code} {login2.text[:100]}")

    # S9-S11 password reset flow
    # Clear cooldown in case a prior send set it without completing.
    rr = redis_client(0)
    rr.delete(f"email:verification:cooldown:reset:{apitest_email}")

    r9 = requests.post(f"{BASE}/v1/auth/email-verifications/reset", params={"email": apitest_email})
    reset_code = get_verification_code("reset", apitest_email, retries=3)
    # Mail provider may reject @example.com as spam (500) AFTER Redis stores the code.
    if r9.status_code == 200:
        record("S9", True, f"200 {r9.text[:100]}")
    elif reset_code:
        record("S9", False, f"{r9.status_code} mail failed but Redis has code={reset_code} (continue S10/S11) {r9.text[:80]}")
    else:
        record("S9", False, f"{r9.status_code} {r9.text[:100]}")
        record("S10", False, "skipped")
        record("S11", False, "skipped")
        return

    if not reset_code:
        record("S10", False, f"Redis miss key=email:verification:reset:{apitest_email}")
        record("S11", False, "skipped")
        return

    r10 = requests.post(
        f"{BASE}/v1/auth/email-verifications/reset/validate",
        params={"email": apitest_email, "code": reset_code},
        headers={"Idempotency-Key": idem_key()},
    )
    record("S10", r10.status_code == 200, f"{r10.status_code} code={reset_code} {r10.text[:100]}")
    if r10.status_code != 200:
        record("S11", False, "skipped")
        return

    r11 = requests.post(
        f"{BASE}/v1/auth/password-resets",
        headers={"Idempotency-Key": idem_key(), "Content-Type": "application/json"},
        json={"email": apitest_email, "newPassword": PASSWORD},
    )
    token_after, _, login_after = login_user(apitest_email, PASSWORD)
    record(
        "S11",
        r11.status_code == 200 and token_after,
        f"reset={r11.status_code} relogin={'ok' if token_after else login_after.status_code} {r11.text[:80]}",
    )


def ensure_admin_token(user_token):
    """Create bootstrap admin if needed, return (admin_token, admin_email)."""
    bootstrap_email = "adminapitest@example.com"
    token, session, resp = login_admin(bootstrap_email, ADMIN_DEFAULT_PASSWORD)
    if token:
        return token, bootstrap_email

    # create via user token
    r = requests.post(
        f"{BASE}/v2/admins",
        headers=auth_headers(user_token, with_idem=True),
        json={
            "email": bootstrap_email,
            "username": "adminapitest",
            "name": "Admin API Test",
        },
    )
    if r.status_code not in (200, 409):
        # 409 may mean already exists with different password
        print(f"  [WARN] bootstrap admin create: {r.status_code} {r.text[:150]}")

    token, session, resp = login_admin(bootstrap_email, ADMIN_DEFAULT_PASSWORD)
    if token:
        return token, bootstrap_email

    # try with explicit password create unique admin
    email = f"adminboot-{int(time.time())}@example.com"
    r2 = requests.post(
        f"{BASE}/v2/admins",
        headers=auth_headers(user_token, with_idem=True),
        json={
            "email": email,
            "username": f"adminboot{int(time.time())}",
            "name": "Admin Boot",
            "password": ADMIN_DEFAULT_PASSWORD,
        },
    )
    print(f"  [INFO] create unique admin: {r2.status_code} {r2.text[:120]}")
    token, _, resp = login_admin(email, ADMIN_DEFAULT_PASSWORD)
    return token, email if token else (None, None)


def run_s12_and_admin():
    print("\n=== S12 ADMIN login + A1-A6 CRUD ===")
    user_token, _, login_resp = login_user()
    if not user_token:
        record("S12", False, f"need user token for bootstrap: {login_resp.status_code}")
        for c in ("A1", "A2", "A3", "A4", "A5", "A6"):
            record(c, False, "skipped")
        return

    admin_token, admin_email = ensure_admin_token(user_token)
    record("S12", bool(admin_token), f"admin_email={admin_email} token={'yes' if admin_token else 'no'}")
    if not admin_token:
        for c in ("A1", "A2", "A3", "A4", "A5", "A6"):
            record(c, False, "skipped: no admin token")
        return

    ts = int(time.time())
    email1 = f"admintmp-{ts}-a@example.com"
    # A1 create
    r1 = requests.post(
        f"{BASE}/v2/admins",
        headers=auth_headers(admin_token, with_idem=True),
        json={
            "email": email1,
            "username": f"admintmp{ts}a",
            "name": "Temp Admin A",
            "password": ADMIN_DEFAULT_PASSWORD,
        },
    )
    record("A1", r1.status_code == 200, f"{r1.status_code} {r1.text[:120]}")

    # find id via list
    r2 = requests.get(f"{BASE}/v2/admins", headers=auth_headers(admin_token))
    admin_id = None
    if r2.status_code == 200:
        data = r2.json().get("data") or []
        for a in data:
            if a.get("email") == email1:
                admin_id = a.get("id")
                break
    record("A2", r2.status_code == 200 and admin_id is not None, f"{r2.status_code} id={admin_id} count={len(r2.json().get('data') or []) if r2.status_code==200 else 0}")
    if admin_id:
        created_admin_ids.append(admin_id)

    if not admin_id:
        for c in ("A3", "A4", "A5", "A6"):
            record(c, False, "skipped: no admin id")
        return

    r3 = requests.get(f"{BASE}/v2/admins/{admin_id}", headers=auth_headers(admin_token))
    record("A3", r3.status_code == 200 and (r3.json().get("data") or {}).get("email") == email1, f"{r3.status_code} {r3.text[:120]}")

    new_name = "Temp Admin A Updated"
    r4 = requests.put(
        f"{BASE}/v2/admins/{admin_id}",
        headers=auth_headers(admin_token, with_idem=True),
        json={"name": new_name, "email": email1, "username": f"admintmp{ts}a"},
    )
    r4g = requests.get(f"{BASE}/v2/admins/{admin_id}", headers=auth_headers(admin_token))
    name_ok = False
    if r4g.status_code == 200:
        name_ok = (r4g.json().get("data") or {}).get("name") == new_name
    record("A4", r4.status_code == 200 and name_ok, f"put={r4.status_code} name_ok={name_ok}")

    r5 = requests.delete(
        f"{BASE}/v2/admins/{admin_id}",
        headers=auth_headers(admin_token, with_idem=True),
    )
    r5g = requests.get(f"{BASE}/v2/admins/{admin_id}", headers=auth_headers(admin_token))
    # after delete, get may 200 with null or 404/error
    gone = r5g.status_code != 200 or r5g.json().get("data") is None
    record("A5", r5.status_code == 200 and gone, f"del={r5.status_code} get={r5g.status_code} {r5g.text[:80]}")
    if admin_id in created_admin_ids:
        created_admin_ids.remove(admin_id)

    # A6 batch: create 2 then delete
    ids = []
    for suf in ("b", "c"):
        em = f"admintmp-{ts}-{suf}@example.com"
        rr = requests.post(
            f"{BASE}/v2/admins",
            headers=auth_headers(admin_token, with_idem=True),
            json={
                "email": em,
                "username": f"admintmp{ts}{suf}",
                "name": f"Temp {suf}",
                "password": ADMIN_DEFAULT_PASSWORD,
            },
        )
        if rr.status_code != 200:
            record("A6", False, f"create {suf} failed: {rr.status_code} {rr.text[:80]}")
            return
    rlist = requests.get(f"{BASE}/v2/admins", headers=auth_headers(admin_token))
    for a in rlist.json().get("data") or []:
        if a.get("email", "").startswith(f"admintmp-{ts}-"):
            ids.append(a["id"])
    if len(ids) < 2:
        record("A6", False, f"expected >=2 temp admins, got {ids}")
        return
    r6 = requests.delete(
        f"{BASE}/v2/admins/batch",
        headers=auth_headers(admin_token, with_idem=True),
        json=ids,
    )
    record("A6", r6.status_code == 200, f"{r6.status_code} deleted={ids} {r6.text[:80]}")


def cleanup():
    print("\n=== Cleanup ===")
    # restore regtest1 password just in case
    bust_user_cache(REGTEST_EMAIL)
    token, _, resp = login_user()
    if not token:
        # try nothing
        print(f"  regtest1 login: {resp.status_code}")
    else:
        print("  regtest1 password OK")

    # delete apitest user if we have id
    if created_user_id and token:
        r = requests.delete(
            f"{BASE}/v2/users/{created_user_id}",
            headers=auth_headers(token, with_idem=True),
        )
        print(f"  delete apitest user {created_user_id}: {r.status_code}")

    # leftover admins
    if token:
        admin_token, _ = ensure_admin_token(token)
        if admin_token and created_admin_ids:
            for aid in list(created_admin_ids):
                r = requests.delete(
                    f"{BASE}/v2/admins/{aid}",
                    headers=auth_headers(admin_token, with_idem=True),
                )
                print(f"  delete leftover admin {aid}: {r.status_code}")


def main():
    print("=" * 60)
    print("Session + Admin API Integration Tests")
    print("=" * 60)

    if not wait_healthy():
        print("FAIL: Spring Boot not healthy")
        sys.exit(1)
    print("Healthy: GET /api/v1 = 200")

    run_s1()
    run_s2_s4()
    run_s5_s11()
    run_s12_and_admin()
    cleanup()

    print("\n" + "=" * 60)
    passed = sum(1 for _, s, _ in results if s == "PASS")
    failed = sum(1 for _, s, _ in results if s == "FAIL")
    print(f"Results: {passed} passed, {failed} failed, {len(results)} total")
    for case_id, status, detail in results:
        print(f"  {status:4} {case_id}: {detail[:100]}")
    print("=" * 60)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
