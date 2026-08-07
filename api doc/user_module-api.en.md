# User Module API Reference (Frontend)

For frontend integration (profile, avatar, account admin).  
**Login / register / refresh / logout / verification / password / managed users** live in: [`auth_module-api.en.md`](./auth_module-api.en.md).

Base URL: `https://dev.xlearnedu.com:8080/api`

Local: `http://localhost:8080/api`

---

## Table of contents

1. [How to call](#1-how-to-call)
2. [Typical page flows](#2-typical-page-flows)
3. [Roles and permissions (product rules)](#3-roles-and-permissions-product-rules)
4. [Enum cheat sheet](#4-enum-cheat-sheet)
5. [Auth (see Auth docs)](#5-auth-see-auth-docs)
6. [Email verification and password (see Auth docs)](#6-email-verification-and-password-see-auth-docs)
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

**Write APIs that require `Idempotency-Key` (this module):**

- `PUT /v1/auth/password`, `POST /v1/auth/password-resets` (see Auth docs)
- `PATCH /v2/me/profile`
- `DELETE /v2/me/profile/avatar`
- `POST/PUT/DELETE/PATCH /v2/users...` (except GET)
- Managed-user writes (see Auth docs)

Missing key → `IDEMPOTENCY_KEY_REQUIRED`.

**Does not require** a key: `login` / `register` / `refresh-token` / `logout`, send verification code, `PUT .../avatar`, all GETs.

> **`.../email-verifications/*/validate` endpoints are removed**; codes are consumed inside register / password-resets.

Invalid token → `401` (`INVALID_TOKEN` / `UNAUTHORIZED`).

### 1.2 Anonymous (JWT not checked)

- `GET /v1`
- `POST /v1/auth/login`, `register`, `refresh-token`, `logout`
- `POST /v1/auth/email-verifications/register`, `.../reset`
- `POST /v1/auth/password-resets`
- `GET /v2/users/{userId}/avatar`

Note: `PUT /v1/auth/password` **requires** Bearer; `logout` does **not** (Cookie-based).

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

### 2.1 Login / register / refresh / logout / password

Full flow diagrams and request fields: [`auth_module-api.en.md` §2](./auth_module-api.en.md#2-typical-business-flows). Summary:

| Flow | API order |
|------|----------|
| Login | `POST /v1/auth/login` → store `accessToken` → (optional) `GET /v2/me/profile` |
| Register | send code → `POST /v1/auth/register` (includes `verificationCode`; **no** validate) |
| Refresh | Cookie → `POST /v1/auth/refresh-token` |
| Logout | `POST /v1/auth/logout` (anonymous OK; Cookie) |
| Forgot password | send reset code → `POST /v1/auth/password-resets` (includes `verificationCode`) |
| Change password | `PUT /v1/auth/password` (`currentPassword` / `newPassword`) |

### 2.2 Profile / avatar

1. `GET /v2/me/profile`
2. Update display name / email notifications: `PATCH /v2/me/profile` (Idempotency-Key required)
3. Change avatar: `PUT /v2/me/profile/avatar` (`multipart`, field name `file`)
4. Remove avatar: `DELETE /v2/me/profile/avatar` (Idempotency-Key required)
5. `<img src="{avatarUrl}">`; for others use the returned URL or `GET /v2/users/{id}/avatar`

---

## 3. Roles and permissions (product rules)

Full login routing and capability matrix: [`auth_module-api.en.md` §3](./auth_module-api.en.md#3-roles-and-login-routing). Summary:

| Field | Values | Meaning |
|------|--------|----------|
| Login body `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` \| `ADMIN` (legacy) | Selects user vs admin table |
| JWT `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` | Authorization role |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR`, etc. | User table; register defaults to `STUDENT` |

| Capability | USER | TENANT_ADMIN | SYSTEM_ADMIN |
|------------|:----:|:------------:|:------------:|
| Self-register / profile / avatar | ✓ | profile ✓ | no `/me/profile` |
| `/v2/users` CRUD | product: console | | recommended |
| `/v2/admins` read | | | ✓ (writes disabled) |
| Managed users | | tenant scope | system scope |

> Course-level Instructor / TA / Student is documented in the Course API reference; that is **not** the same as platform `level` here.

Common errors: `INVALID_CREDENTIALS`, `USER_NOT_FOUND`, `UNAUTHORIZED`, `INVALID_TOKEN`.

---

## 4. Enum cheat sheet

| Field | Allowed values |
|------|--------|
| Login body `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` \| `ADMIN` |
| JWT / Profile `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `NOT_APPLICABLE`, etc. |
| List teachers query | `role=instructor` or `role=teacher` (`GET /v2/users`) |

Invalid / missing required → usually `PARAM_MISSING` or `400 BAD_REQUEST`.

### 4.1 `tenantId`

`user.tenant_id` is **NOT NULL**; existing rows were backfilled to `1`.

| Scenario | Rule |
|------|------|
| Public `POST /v1/auth/register` | Server **always binds tenant 1** (ignores client `tenantId`) |
| Admin `POST /v2/users` | Body **requires** `tenantId` for any existing tenant; missing → `PARAM_MISSING`; unknown → `TENANT_NOT_FOUND` |
| Change user tenant | **Only** `PATCH /v2/admin/users/{id}/tenant` (SYSTEM_ADMIN + Idempotency-Key). Enrollment / instructing / created courses → `409 USER_TENANT_CHANGE_BLOCKED` |
| `PUT /v2/users/{id}` | Body **must not** include `tenantId` (if present → `400 BAD_REQUEST`) |
| Profile / password / avatar | Do **not** read or write tenant |

See also [`tenant_module-api.md`](./tenant_module-api.md).

---

## 5. Auth (see Auth docs)

Session, login/register/refresh/logout request/response/error codes:

→ [`auth_module-api.en.md` §4 Session API](./auth_module-api.en.md#4-session-api)

This document no longer duplicates Auth endpoint details.

---

## 6. Email verification and password (see Auth docs)

Send code, change password, reset (includes `verificationCode`; no validate endpoints):

→ [`auth_module-api.en.md` §5](./auth_module-api.en.md#5-email-verification-and-password)

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
| POST | `/v2/users` | Create; at least `email`+`password`+**`tenantId`**; `role` forced to `USER` |
| GET | `/v2/users/{id}` | Detail; `USER_NOT_FOUND` |
| GET | `/v2/users` | List; optional User field query filters; `?role=instructor` / `teacher` → teachers only |
| PUT | `/v2/users/{id}` | Update; **cannot** change `tenantId` (body includes it → `400`) |
| DELETE | `/v2/users/{id}` | Delete |
| DELETE | `/v2/users/batch` | Body: `[1,2,3]` |
| PATCH | `/v2/users/{id}/password-status` | Sets `mustChangePassword=false` |
| PATCH | `/v2/admin/users/{id}/tenant` | **Admin only**; change tenant (below) |

Common `User` fields: `id` **`tenantId`** `username` `password` `name` `avatar` (**key**) `role` `level` `email` `mustChangePassword` `emailNotifications`.

**Create body notes**

| Field | Required | Notes |
|------|------|------|
| email / password | Yes | |
| tenantId | **Yes** | Any existing tenant; missing → `PARAM_MISSING`; unknown → `TENANT_NOT_FOUND`. Frontend currently hard-codes `1` |
| name / username / level | No | `level` defaults to `STUDENT` |

```json
{
  "email": "tzuser@example.com",
  "password": "Test12345",
  "name": "TZ User",
  "username": "tzuser",
  "level": "INSTRUCTOR",
  "tenantId": 1
}
```

### 9.1 Change user tenant — `PATCH /v2/admin/users/{id}/tenant`

| | |
|--|--|
| Auth | JWT `role=SYSTEM_ADMIN`; else `403 ACCESS_DENIED` |
| Idempotency-Key | **Yes** |

**Body**

```json
{ "tenantId": 1 }
```

- `tenantId` required; tenant must exist
- Target user has enrollment, or is instructor/creator of a course → `409 USER_TENANT_CHANGE_BLOCKED`
- Same as current value → success (idempotent)

> Admin APIs return `avatar` as a storage key, not an `<img>` URL. Use §8.3 for display.  
> Do not show `password` in the UI.

Example errors: `USER_ALREADY_EXISTS`, `PARAM_MISSING`, `INVALID_PASSWORD_FORMAT`, `USER_NOT_FOUND`, `TENANT_NOT_FOUND`, `USER_TENANT_CHANGE_BLOCKED`, `ACCESS_DENIED`.

---

## 10. Admins management

Prefix: `/v2/admins`. Authz: `SYSTEM_ADMIN`.  
**Reads available; writes are `403 Forbidden` until Phase 2.**  
Details and managed users: [`auth_module-api.en.md` §6–§7](./auth_module-api.en.md#6-admins-read-only).

| Method | Path | Notes |
|------|------|------|
| GET | `/v2/admins/{id}` | One admin |
| GET | `/v2/admins` | List; query filters |
| POST / PUT / DELETE | `/v2/admins...` | **Disabled** |

Admins have **no** `/v2/me/profile`. Login with `role: "ADMIN"` or `"SYSTEM_ADMIN"`.

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

Full Auth cheat sheet: [`auth_module-api.en.md` §8](./auth_module-api.en.md#8-endpoint-cheat-sheet). This module:

| Method | Path | Who | Idempotency-Key |
|------|------|------|----------|
| GET | `/v2/me/profile` | Current USER / TENANT_ADMIN | |
| PATCH | `/v2/me/profile` | Current USER / TENANT_ADMIN | Yes |
| PUT | `/v2/me/profile/avatar` | Current USER / TENANT_ADMIN | No |
| DELETE | `/v2/me/profile/avatar` | Current USER / TENANT_ADMIN | Yes |
| GET | `/v2/users/{userId}/avatar` | Public | |
| GET | `/v2/users`, `/v2/users/{id}` | Logged in (product: console) | |
| POST/PUT/DELETE/PATCH | `/v2/users...` | Logged in (product: console) | Yes |
| PATCH | `/v2/admin/users/{id}/tenant` | SYSTEM_ADMIN | Yes |
| GET | `/v2/admins`, `/v2/admins/{id}` | SYSTEM_ADMIN | |

---

## 13. Local test accounts

| Use | email | password | Notes |
|------|-------|----------|------|
| Student | `regtest1@example.com` … `regtest5@example.com` | `Test12345` | `role=USER`, `level=STUDENT` |
| Instructor | `teachtest2@example.com` | `Test12345` | `role=USER`, `level=INSTRUCTOR` |
| Platform admin | `admin@example.com` | `Test12345` | login `role=ADMIN` or `SYSTEM_ADMIN` |

More Auth notes: [`auth_module-api.en.md` §9](./auth_module-api.en.md#9-local-test-accounts).


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
