# User Module API Reference (Frontend)

For frontend integration and page implementation (login/register, profile, avatar, account admin).  
Base URL: `https://dev.xlearnedu.com:8080/api`

Local: `http://localhost:8080/api`

---

## Table of contents

1. [How to call](#1-how-to-call)
2. [Typical page flows](#2-typical-page-flows)
3. [Roles and permissions (product rules)](#3-roles-and-permissions-product-rules)
4. [Enum cheat sheet](#4-enum-cheat-sheet)
5. [Auth: login / register / logout / refresh](#5-auth-login--register--logout--refresh)
6. [Email verification and password](#6-email-verification-and-password)
7. [Profile (me)](#7-profile-me)
8. [Avatar](#8-avatar)
9. [Users account management](#9-users-account-management)
10. [Admins management](#10-admins-management)
11. [Handling avatar binary](#11-handling-avatar-binary)
12. [Endpoint cheat sheet](#12-endpoint-cheat-sheet)
13. [Local test accounts](#13-local-test-accounts)

---

## 1. How to call

### 1.1 Headers

| Header | When required |
|--------|----------|
| `Authorization: Bearer {accessToken}` | Almost all APIs (login first via `POST /v1/auth/login` or register) |
| `Idempotency-Key: {uuid}` | **Only the write APIs listed below** (new UUID per request) |
| `Content-Type: application/json` | JSON body |
| `Content-Type: multipart/form-data` | Avatar upload (browser FormData sets this automatically) |

**Write APIs that require `Idempotency-Key`:**

- `POST /v1/auth/email-verifications/register/validate`
- `POST /v1/auth/email-verifications/reset/validate`
- `PUT /v1/auth/password`
- `POST /v1/auth/password-resets`
- `PATCH /v2/me/profile`
- `DELETE /v2/me/profile/avatar`
- `POST/PUT/DELETE/PATCH /v2/users...` (except GET)
- `POST/PUT/DELETE /v2/admins...` (except GET)

Missing key → `IDEMPOTENCY_KEY_REQUIRED`.

**Does not require** a key: `login` / `register` / `refresh-token` / `logout`, send verification code, `PUT .../avatar`, all GETs.

Invalid token → `401` (`INVALID_TOKEN` / `UNAUTHORIZED`).

### 1.2 Anonymous (JWT not checked)

- `GET /v1`
- `POST /v1/auth/login`, `register`, `refresh-token`
- `POST /v1/auth/email-verifications/**`
- `POST /v1/auth/password-resets`
- `GET /v2/users/{userId}/avatar`

Note: `PUT /v1/auth/password` and `POST /v1/auth/logout` **require** Bearer.

### 1.3 Cookie: `refreshToken`

| | |
|--|--|
| Set by | Successful `login` / `register` / `refresh-token` via `Set-Cookie` |
| Attributes | HttpOnly, Secure, SameSite=Lax, path=/, ~14 days |
| In JSON? | **No.** `AuthResult.refreshToken` is write-only for the server cookie; not in the response body |
| Frontend | Same-origin requests send the cookie by default; `refresh-token` relies on the cookie — do not put refresh into a header |

### 1.4 Unified JSON response

On success read `data`; on failure read `code` (do not rely on HTTP status alone).

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": {},
  "message": "Success",
  "timestamp": "2026-07-25T01:00:00Z"
}
```

```json
{
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "...",
  "timestamp": "2026-07-25T01:00:00Z"
}
```

Avatar binary streams are **not** this JSON envelope — see [§11](#11-handling-avatar-binary).

### 1.5 Field name notes

| API | Avatar field |
|------|----------|
| `POST /v1/auth/login` (USER) / `register` | `avatar` (already a usable proxy URL) |
| `GET/PATCH /v2/me/profile`, upload/delete avatar | `avatarUrl` |
| `GET /v2/users/{id}` admin APIs | `avatar` (**MinIO object key** in DB, not a URL) |

For display, use the `avatar` / `avatarUrl` from the response, or `GET /v2/users/{userId}/avatar`. **Do not** treat the admin API key as an image URL.

### 1.6 Password rules

At least **8** characters, and must contain both **letters** and **digits**. Otherwise `INVALID_PASSWORD_FORMAT`.

---

## 2. Typical page flows

### 2.1 Login

1. `POST /v1/auth/login` (`role: "USER"`) → store `data.accessToken`
2. Cookie automatically holds `refreshToken` (do not read it from JSON)
3. Profile page: `GET /v2/me/profile`
4. Before access token expires: `POST /v1/auth/refresh-token` (cookie) → `data` is the new accessToken string
5. Logout: `POST /v1/auth/logout` (Bearer required)

### 2.2 Register

1. `POST /v1/auth/email-verifications/register?email=...` send code
2. `POST /v1/auth/email-verifications/register/validate?email=...&code=...` (Idempotency-Key required)
3. `POST /v1/auth/register` (`email` `password` `name`; optional `username`)
4. Same as login on success: `accessToken` + refresh cookie; fixed `role=USER`, `level=STUDENT`

### 2.3 Profile / avatar

1. `GET /v2/me/profile`
2. Update display name / email notifications: `PATCH /v2/me/profile` (Idempotency-Key required)
3. Change avatar: `PUT /v2/me/profile/avatar` (`multipart`, field name `file`)
4. Remove avatar: `DELETE /v2/me/profile/avatar` (Idempotency-Key required)
5. `<img src="{avatarUrl}">`; for others use the returned URL or `GET /v2/users/{id}/avatar`

### 2.4 Forgot password

1. `POST /v1/auth/email-verifications/reset?email=...`
2. `POST /v1/auth/email-verifications/reset/validate?email=...&code=...` (Idempotency-Key required)
3. `POST /v1/auth/password-resets` with body `{ "email", "newPassword" }` (Idempotency-Key required)

### 2.5 Change password while logged in

1. `PUT /v1/auth/password` (Bearer + Idempotency-Key)
2. Body: `email`, `password` (old), `newPassword`, `role` (`USER` or `ADMIN`)
3. On success that user’s refresh tokens are cleared — login again for a new cookie

---

## 3. Roles and permissions (product rules)

Two layers of platform identity:

| Field | Values | Meaning |
|------|--------|----------|
| `role` | `USER` \| `ADMIN` | Platform role in login body / JWT; **must match account type** |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `SELF` | Meaningful for USER; register defaults to `STUDENT` |

| Capability | Student / instructor (USER) | Platform Admin |
|------|:------------------:|:----------:|
| Login / refresh / logout | ✓ (`role=USER`) | ✓ (`role=ADMIN`) |
| Self-register | ✓ | |
| View / edit own profile & avatar | ✓ | (Admin uses `/v2/admins`; no `/me/profile`) |
| Change own password | ✓ | ✓ |
| `/v2/users` CRUD, list teachers | **No Admin gate in code**; **product: admin console only** | ✓ (recommended) |
| `/v2/admins` CRUD | Same — do not expose to students | ✓ |

> Course-level Instructor / TA / Student is documented in the Course API reference; that is **not** the same as platform `level` here.

Common errors: `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `USER_NOT_FOUND`, `UNAUTHORIZED`, `INVALID_TOKEN`.

---

## 4. Enum cheat sheet

| Field | Allowed values |
|------|--------|
| `role` (login / AuthResult / Profile) | `USER` \| `ADMIN` |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `SELF` |
| List teachers query | `role=instructor` or `role=teacher` (`GET /v2/users`) |

Invalid / missing required → usually `PARAM_MISSING` or `400 BAD_REQUEST`.

---

## 5. Auth: login / register / logout / refresh

Prefix: `/v1/auth` (health: `GET /v1`)

### 5.1 Health — `GET /v1`

No auth. Success: `data` is `"访问成功"`.

---

### 5.2 Login — `POST /v1/auth/login`

| | |
|--|--|
| Auth | No |
| Idempotency-Key | No |

**Body**

| Field | Type | Required | Notes |
|------|------|------|------|
| email | string | Yes | |
| password | string | Yes | |
| role | string | Yes | `USER` or `ADMIN`; must match account type |

```json
{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "role": "USER"
}
```

Success `data` (`AuthResult`):

```json
{
  "userId": 385,
  "email": "regtest1@example.com",
  "name": "Alex Rivera",
  "username": "regtest1",
  "role": "USER",
  "level": "STUDENT",
  "avatar": "https://dev.xlearnedu.com:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f",
  "accessToken": "eyJ..."
}
```

- Also `Set-Cookie: refreshToken=...`
- `avatar` may be `null` if none
- For ADMIN login, `avatar` may be the raw DB value, not necessarily a proxy URL

Errors: `PARAM_MISSING`, `USER_NOT_FOUND`, `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED` (~≥6 consecutive failures locks for a while), `TOKEN_CREATION_FAILED`.

---

### 5.3 Register — `POST /v1/auth/register`

| | |
|--|--|
| Auth | No |
| Idempotency-Key | No |
| Prerequisite | Complete register email verification first (~15 minutes) |

**Body**

| Field | Type | Required | Notes |
|------|------|------|------|
| email | string | Yes | |
| password | string | Yes | See §1.6 |
| name | string | Yes | Display name |
| username | string | No | Defaults to the part before `@` in email |

Success: same shape as login `AuthResult` + refresh cookie.  
Fixed: `role=USER`, `level=STUDENT`, `emailNotifications=true`.

Errors: `PARAM_MISSING`, `INVALID_VERIFICATION_CODE` (not verified), `INVALID_PASSWORD_FORMAT`, `BAD_REQUEST` (e.g. email already exists; message may be anti-enumeration).

---

### 5.4 Refresh token — `POST /v1/auth/refresh-token`

| | |
|--|--|
| Auth | No (Cookie `refreshToken`) |
| Idempotency-Key | No |
| Rate limit | Same IP ~>10 times / ~60s → `TOO_MANY_REQUESTS` |

Success: `data` is the **new accessToken string** (not an object); refresh cookie is rotated.

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": "eyJ...",
  "message": "Success"
}
```

Errors: `REFRESH_TOKEN_INVALID`, `TOO_MANY_REQUESTS`.

---

### 5.5 Logout — `POST /v1/auth/logout`

| | |
|--|--|
| Auth | **Bearer required** |
| Idempotency-Key | No |

Deletes server-side refresh and clears the cookie. Success: `data` is `null`.

---

## 6. Email verification and password

### 6.1 Send register code — `POST /v1/auth/email-verifications/register`

| | |
|--|--|
| Auth | No |
| Params | `email` (query / form; implementation has no `@RequestParam` — prefer query `?email=`) |

Already-registered emails still return success (silent, anti-enumeration).

Rate-limit errors: `VERIFICATION_RESEND_COOLDOWN` (~60s), `VERIFICATION_HOURLY_LIMIT` (~5 / hour).

---

### 6.2 Validate register code — `POST /v1/auth/email-verifications/register/validate`

| | |
|--|--|
| Auth | No |
| Idempotency-Key | **Yes** |
| Params | query: `email`, `code` |

On success writes a “verified” flag (~15 minutes), then call register.

Errors: `INVALID_VERIFICATION_CODE`, `VERIFICATION_CODE_EXPIRED` (code ~10 minutes), `VERIFICATION_ATTEMPTS_EXCEEDED` (~5 wrong tries), `PARAM_MISSING`, `IDEMPOTENCY_*`.

---

### 6.3 Send reset code — `POST /v1/auth/email-verifications/reset`

Same shape as §6.1; unknown users also succeed silently.

---

### 6.4 Validate reset code — `POST /v1/auth/email-verifications/reset/validate`

Same as §6.2; Idempotency-Key required. Then call §6.6.

---

### 6.5 Change password (logged in) — `PUT /v1/auth/password`

| | |
|--|--|
| Auth | **Bearer required** |
| Idempotency-Key | **Yes** |

**Body**

| Field | Type | Required | Notes |
|------|------|------|------|
| email | string | Yes | |
| password | string | Yes | Old password |
| newPassword | string | Yes | New password; see §1.6 |
| role | string | Recommended | `USER` or `ADMIN` |

```json
{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "newPassword": "Test12345a",
  "role": "USER"
}
```

Errors: `PARAM_MISSING`, `USER_NOT_FOUND`, `INVALID_PASSWORD`, `INVALID_PASSWORD_FORMAT`, `IDEMPOTENCY_*`.

---

### 6.6 Forgot-password reset — `POST /v1/auth/password-resets`

| | |
|--|--|
| Auth | No |
| Idempotency-Key | **Yes** |
| Prerequisite | Complete reset email verification first |

**Body**

```json
{
  "email": "regtest1@example.com",
  "newPassword": "Test12345a"
}
```

Errors: `PARAM_MISSING`, `INVALID_VERIFICATION_CODE`, `INVALID_PASSWORD_FORMAT`, `BAD_REQUEST`.

---

## 7. Profile (me)

Prefix: `/v2/me/profile`  
Identity: JWT → current `userId`; otherwise `UNAUTHORIZED`.

### 7.1 Get — `GET /v2/me/profile`

| | |
|--|--|
| Auth | Bearer required |
| Idempotency-Key | No |

Success `data` (`ProfileResponse`):

```json
{
  "userId": 385,
  "displayName": "Alex Rivera",
  "email": "regtest1@example.com",
  "role": "USER",
  "level": "STUDENT",
  "avatarUrl": "https://dev.xlearnedu.com:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f",
  "emailNotifications": true
}
```

- No avatar → `avatarUrl` is `null`
- If DB `emailNotifications` is null, response uses **true**

---

### 7.2 Update — `PATCH /v2/me/profile`

| | |
|--|--|
| Auth | Bearer required |
| Idempotency-Key | **Yes** |
| Body | At least one field |

| Field | Type | Notes |
|------|------|------|
| displayName | string | After trim, 1–100 chars; maps to DB `name` |
| emailNotifications | boolean | Whether to receive email notifications |

```json
{
  "displayName": "Alex Rivera",
  "emailNotifications": false
}
```

Success: `data` is the updated `ProfileResponse`.

Errors: `PARAM_MISSING` (no fields), `BAD_REQUEST` (invalid displayName), `USER_NOT_FOUND`, `IDEMPOTENCY_*`.

---

## 8. Avatar

### 8.1 Upload own avatar — `PUT /v2/me/profile/avatar`

| | |
|--|--|
| Auth | Bearer required |
| Idempotency-Key | **No** |
| Content-Type | `multipart/form-data` |
| Field name | **`file`** (`@RequestPart("file")`) |

Limits:

- Max **5MB**
- Types: JPG / JPEG / PNG (Content-Type or extension)

Success: `data` is `ProfileResponse` (new `avatarUrl` with a new `?v=` cache buster).

Errors: `INVALID_AVATAR_FILE`, `USER_NOT_FOUND`.

```js
const form = new FormData();
form.append("file", fileInput.files[0]); // field name must be file

await fetch(`${base}/v2/me/profile/avatar`, {
  method: "PUT",
  headers: { Authorization: `Bearer ${token}` },
  // Do not set Content-Type manually; let the browser set the boundary
  body: form,
});
```

---

### 8.2 Delete own avatar — `DELETE /v2/me/profile/avatar`

| | |
|--|--|
| Auth | Bearer required |
| Idempotency-Key | **Yes** |

Deleting when there is no avatar still returns `200` (idempotent). Success: `avatarUrl` is `null`.

---

### 8.3 Stream avatar — `GET /v2/users/{userId}/avatar`

| | |
|--|--|
| Auth | **Public** (Bearer optional) |
| Success body | Image bytes, **not** `ApiResponse` |
| Cache | `Cache-Control: private, max-age=300` |
| Content-Type | `image/jpeg` or `image/png` |

Optional query: `?v={uuid}` (already present on login/profile URLs; cache busting).

No user / no avatar / fetch failure → `NOT_FOUND`.

**URL shape** (server `AvatarUrlBuilder`):

```text
{contextPath}/v2/users/{userId}/avatar?v={uuid-from-object-key-filename}
```

Example: `https://dev.xlearnedu.com:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f`

DB stores the MinIO key (e.g. `385/xxxx.png`); the frontend should only use the full URL from responses.

---

## 9. Users account management

Prefix: `/v2/users`  
Bearer required. Writes need Idempotency-Key.  
**Product rule: admin console only**; code currently does not enforce Admin.

| Method | Path | Notes |
|------|------|------|
| POST | `/v2/users` | Create; at least `email`+`password`; `role` forced to `USER` |
| GET | `/v2/users/{id}` | Detail; `USER_NOT_FOUND` |
| GET | `/v2/users` | List; optional User field query filters; `?role=instructor` / `teacher` → teachers only |
| PUT | `/v2/users/{id}` | Update |
| DELETE | `/v2/users/{id}` | Delete |
| DELETE | `/v2/users/batch` | Body: `[1,2,3]` |
| PATCH | `/v2/users/{id}/password-status` | Sets `mustChangePassword=false` |

Common `User` fields: `id` `username` `password` `name` `avatar` (**key**) `role` `level` `email` `mustChangePassword` `emailNotifications`.

> Admin APIs return `avatar` as a storage key, not an `<img>` URL. Use §8.3 for display.  
> Do not show `password` in the UI.

Example errors: `USER_ALREADY_EXISTS`, `PARAM_MISSING`, `INVALID_PASSWORD_FORMAT`, `USER_NOT_FOUND`.

---

## 10. Admins management

Prefix: `/v2/admins`  
Symmetric CRUD; Bearer required; writes need Idempotency-Key.  
**Product rule: platform Admin console only.**

| Method | Path | Notes |
|------|------|------|
| POST | `/v2/admins` | Create; duplicate email → `USER_ALREADY_EXISTS`; missing password may use a server default |
| GET | `/v2/admins/{id}` | |
| GET | `/v2/admins` | Query filters |
| PUT | `/v2/admins/{id}` | |
| DELETE | `/v2/admins/{id}` | |
| DELETE | `/v2/admins/batch` | Body: id list |

Common fields: `id` `username` `password` `name` `avatar` `role` `phone` `email` `invitation`, etc.

Admins have **no** `/v2/me/profile`; update via this CRUD. Login with `role: "ADMIN"`.

---

## 11. Handling avatar binary

Applies to: `GET /v2/users/{userId}/avatar`.

| Point | Notes |
|----|------|
| Auth | Public; Bearer optional |
| Success body | Image bytes, **not** `{ status, code, data }` |
| Recommended | Put `avatarUrl` / `avatar` into `<img src>` |
| Errors | May still be JSON (e.g. `NOT_FOUND`); check `Content-Type` first |
| Cache | After upload, `?v=` changes; old URLs may cache ~5 minutes |

```js
// Profile page: prefer avatarUrl from the API
<img src={profile.avatarUrl} alt="" />

// After upload, replace local state with the new avatarUrl from the response
```

---

## 12. Endpoint cheat sheet

| Method | Path | Who | Idempotency-Key |
|------|------|------|----------|
| GET | `/v1` | Anonymous | |
| POST | `/v1/auth/login` | Anonymous | |
| POST | `/v1/auth/register` | Anonymous (email verify first) | |
| POST | `/v1/auth/refresh-token` | Cookie | |
| POST | `/v1/auth/logout` | Logged in | |
| POST | `/v1/auth/email-verifications/register` | Anonymous | |
| POST | `/v1/auth/email-verifications/register/validate` | Anonymous | Yes |
| POST | `/v1/auth/email-verifications/reset` | Anonymous | |
| POST | `/v1/auth/email-verifications/reset/validate` | Anonymous | Yes |
| PUT | `/v1/auth/password` | Logged in | Yes |
| POST | `/v1/auth/password-resets` | Anonymous (verify first) | Yes |
| GET | `/v2/me/profile` | Current USER | |
| PATCH | `/v2/me/profile` | Current USER | Yes |
| PUT | `/v2/me/profile/avatar` | Current USER | No |
| DELETE | `/v2/me/profile/avatar` | Current USER | Yes |
| GET | `/v2/users/{userId}/avatar` | Public | |
| GET | `/v2/users`, `/v2/users/{id}` | Logged in (product: Admin) | |
| POST/PUT/DELETE/PATCH | `/v2/users...` | Logged in (product: Admin) | Yes |
| GET/POST/PUT/DELETE | `/v2/admins...` | Logged in (product: Admin) | Writes yes |

---

## 13. Local test accounts

| Use | email | password | Notes |
|------|-------|----------|------|
| Student | `regtest1@example.com` … `regtest5@example.com` | `Test12345` | `role=USER`, `level=STUDENT` |
| Instructor | `teachtest2@example.com` | `Test12345` | `role=USER`, `level=INSTRUCTOR` |

```http
POST /v1/auth/login
Content-Type: application/json

{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "role": "USER"
}
```

Take `data.accessToken` → `Authorization: Bearer ...`.  
Profile: `GET /v2/me/profile`. Avatar: `PUT /v2/me/profile/avatar` (field name `file`).
