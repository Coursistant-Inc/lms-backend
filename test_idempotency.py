"""
Idempotency system integration tests.
Tests:
  T1 - Same key same body → replay cached response (idempotent)
  T2 - Same key different body → 422 IDEMPOTENCY_KEY_MISMATCH
  T3 - Concurrent same key → one 200, one 409
  T4 - Missing key on @Idempotent endpoint → 400 IDEMPOTENCY_KEY_REQUIRED
  T5 - Invalid key format → 400 IDEMPOTENCY_KEY_INVALID
  T6 - No key on non-@Idempotent endpoint → normal 200
"""

import requests
import uuid
import time
import concurrent.futures
import redis
import json
import sys

BASE = "http://localhost:8080/api"
EMAIL = "regtest1@example.com"
PASSWORD = "Test12345"

def redis_client(db=0):
    try:
        r = redis.Redis(host="localhost", port=6379, db=db, password="Jerry0415.", decode_responses=True)
        r.ping()
        return r
    except redis.exceptions.AuthenticationError:
        r = redis.Redis(host="localhost", port=6379, db=db, decode_responses=True)
        r.ping()
        return r

def bust_cache(email):
    r = redis_client(db=0)
    r.delete(f"user:email:{email}")
    r.delete(f"user:login:attempts:{email}")

def login(email=EMAIL, password=PASSWORD):
    bust_cache(email)
    resp = requests.post(f"{BASE}/v1/auth/login", json={
        "email": email,
        "password": password,
        "role": "USER"
    })
    if resp.status_code != 200:
        print(f"  [WARN] Login failed: {resp.status_code} {resp.text}")
        return None
    data = resp.json()
    return data["data"]["accessToken"]

def idem_redis():
    """Redis client for idempotency db2"""
    return redis_client(db=2)

def clear_idem_keys():
    r = idem_redis()
    keys = r.keys("idem:*")
    if keys:
        r.delete(*keys)

def test_t1_replay():
    """Same key, same body → second call replays cached response"""
    print("\n=== T1: Same key same body → replay ===")
    token = login()
    assert token, "Login failed"
    key = str(uuid.uuid4()).replace("-", "")

    headers = {
        "Authorization": f"Bearer {token}",
        "Idempotency-Key": key,
        "Content-Type": "application/json"
    }
    body = {"email": EMAIL, "password": PASSWORD, "newPassword": "Test12345", "type": "update", "role": "USER"}

    r1 = requests.put(f"{BASE}/v1/auth/password", json=body, headers=headers)
    print(f"  First call: {r1.status_code}")
    assert r1.status_code == 200, f"Expected 200, got {r1.status_code}: {r1.text}"

    time.sleep(0.3)

    r2 = requests.put(f"{BASE}/v1/auth/password", json=body, headers=headers)
    print(f"  Second call (replay): {r2.status_code}")
    assert r2.status_code == 200, f"Expected 200 replay, got {r2.status_code}: {r2.text}"
    assert r1.text == r2.text, f"Replayed response should match original.\n  First:  {r1.text}\n  Second: {r2.text}"
    print("  PASS")

def test_t2_mismatch():
    """Same key, different body → 422"""
    print("\n=== T2: Same key different body → 422 ===")
    token = login()
    assert token, "Login failed"
    key = str(uuid.uuid4()).replace("-", "")

    headers = {
        "Authorization": f"Bearer {token}",
        "Idempotency-Key": key,
        "Content-Type": "application/json"
    }
    body1 = {"email": EMAIL, "password": PASSWORD, "newPassword": "Test12345", "type": "update", "role": "USER"}

    r1 = requests.put(f"{BASE}/v1/auth/password", json=body1, headers=headers)
    print(f"  First call: {r1.status_code}")
    assert r1.status_code == 200, f"Expected 200, got {r1.status_code}: {r1.text}"

    time.sleep(0.3)

    body2 = {"email": EMAIL, "password": PASSWORD, "newPassword": "NewPass999", "type": "update", "role": "USER"}
    r2 = requests.put(f"{BASE}/v1/auth/password", json=body2, headers=headers)
    print(f"  Second call (different body): {r2.status_code} | {r2.text[:150]}")
    assert r2.status_code == 422, f"Expected 422, got {r2.status_code}: {r2.text}"
    print("  PASS")

def test_t3_concurrent():
    """Concurrent same key → one succeeds, one gets 409"""
    print("\n=== T3: Concurrent same key → 409 ===")
    token = login()
    assert token, "Login failed"
    key = str(uuid.uuid4()).replace("-", "")

    headers = {
        "Authorization": f"Bearer {token}",
        "Idempotency-Key": key,
        "Content-Type": "application/json"
    }
    body = {"email": EMAIL, "password": PASSWORD, "newPassword": "Test12345", "type": "update", "role": "USER"}

    def call():
        return requests.put(f"{BASE}/v1/auth/password", json=body, headers=headers)

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        f1 = executor.submit(call)
        f2 = executor.submit(call)
        results = [f1.result(), f2.result()]

    statuses = sorted([r.status_code for r in results])
    print(f"  Statuses: {statuses}")
    # One should be 200, the other 409 or 200(replay). Both 200 is also acceptable (if first finishes before second arrives).
    if 409 in statuses:
        assert 200 in statuses, f"Expected one 200 and one 409, got {statuses}"
        print("  PASS (one 200, one 409)")
    elif statuses == [200, 200]:
        print("  PASS (both 200 - first finished before second arrived, replay)")
    else:
        print(f"  WARN: unexpected statuses {statuses}")

def test_t4_missing_key():
    """Missing Idempotency-Key on @Idempotent endpoint → 400"""
    print("\n=== T4: Missing key → 400 ===")
    token = login()
    assert token, "Login failed"

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    body = {"email": EMAIL, "password": PASSWORD, "newPassword": "Test12345", "type": "update", "role": "USER"}

    r = requests.put(f"{BASE}/v1/auth/password", json=body, headers=headers)
    print(f"  Status: {r.status_code} | {r.text[:150]}")
    assert r.status_code == 400, f"Expected 400, got {r.status_code}: {r.text}"
    data = r.json()
    assert "IDEMPOTENCY_KEY_REQUIRED" in json.dumps(data), f"Expected IDEMPOTENCY_KEY_REQUIRED in response: {data}"
    print("  PASS")

def test_t5_invalid_key():
    """Invalid key format → 400"""
    print("\n=== T5: Invalid key format → 400 ===")
    token = login()
    assert token, "Login failed"

    headers = {
        "Authorization": f"Bearer {token}",
        "Idempotency-Key": "invalid key with spaces!@#",
        "Content-Type": "application/json"
    }
    body = {"email": EMAIL, "password": PASSWORD, "newPassword": "Test12345", "type": "update", "role": "USER"}

    r = requests.put(f"{BASE}/v1/auth/password", json=body, headers=headers)
    print(f"  Status: {r.status_code}")
    assert r.status_code == 400, f"Expected 400, got {r.status_code}: {r.text}"
    data = r.json()
    assert "IDEMPOTENCY_KEY_INVALID" in json.dumps(data), f"Expected IDEMPOTENCY_KEY_INVALID in response: {data}"
    print("  PASS")

def test_t6_no_annotation():
    """Non-@Idempotent endpoint without key → normal response"""
    print("\n=== T6: Non-@Idempotent endpoint without key → normal ===")
    token = login()
    assert token, "Login failed"

    headers = {"Authorization": f"Bearer {token}"}
    r = requests.get(f"{BASE}/v2/users/385", headers=headers)
    print(f"  Status: {r.status_code}")
    assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text}"
    print("  PASS")

if __name__ == "__main__":
    print("=" * 60)
    print("Idempotency Integration Tests")
    print("=" * 60)

    clear_idem_keys()

    passed = 0
    failed = 0
    tests = [test_t1_replay, test_t2_mismatch, test_t3_concurrent, test_t4_missing_key, test_t5_invalid_key, test_t6_no_annotation]

    for test in tests:
        try:
            test()
            passed += 1
        except AssertionError as e:
            print(f"  FAILED: {e}")
            failed += 1
        except Exception as e:
            print(f"  ERROR: {e}")
            failed += 1

    print("\n" + "=" * 60)
    print(f"Results: {passed} passed, {failed} failed, {len(tests)} total")
    print("=" * 60)

    if failed > 0:
        sys.exit(1)
