# Course Module API Reference (Frontend)

For frontend integration and page implementation.  
Base URL: `https://dev.xlearnedu.com:8080/api`

---

## Table of contents

1. [How to call](#1-how-to-call)
2. [Typical page flows](#2-typical-page-flows)
3. [Roles and permissions (product rules)](#3-roles-and-permissions-product-rules)
4. [Enum cheat sheet](#4-enum-cheat-sheet)
5. [Course CRUD](#5-course-crud)
6. [Sessions](#6-sessions)
7. [Events](#7-events)
8. [Members / TA](#8-members--ta)
9. [My courses](#9-my-courses)
10. [Admin enroll](#10-admin-enroll)
11. [Syllabus](#11-syllabus)
12. [Weeks](#12-weeks)
13. [Materials](#13-materials)
14. [Handling file streams](#14-handling-file-streams)
15. [Endpoint cheat sheet](#15-endpoint-cheat-sheet)
16. [Local test accounts](#16-local-test-accounts)

---

## 1. How to call

### 1.1 Headers

| Header | When required |
|--------|----------|
| `Authorization: Bearer {accessToken}` | Almost all APIs (login first via `POST /v1/auth/login`) |
| `Idempotency-Key: {uuid}` | **Only the write APIs listed below** (new UUID per request) |
| `Content-Type: application/json` | JSON body |
| `Content-Type: multipart/form-data` | File uploads (browser FormData sets this automatically) |

**Write APIs that require `Idempotency-Key`:**

- `POST/PUT /v2/courses`, `POST /v2/courses/{id}/archive`, `POST .../unarchive`, `POST .../transfer-instructor`
- `POST/PUT .../sessions`
- `POST/PUT .../events`
- `POST/PATCH .../members/.../ta...`, `DELETE .../members/{userId}`
- `POST /v2/admin/courses/{id}/enrollments`, `POST .../enrollments/batch`, `DELETE .../enrollments/{userId}`
- `POST .../syllabus`, `POST .../syllabus/restore`, `DELETE .../syllabus`
- `POST/PATCH/PUT .../weeks...` (including publish/unpublish/reorder; not DELETE)
- `PATCH/PUT/POST .../materials` for rename / reorder / move (**creating materials does not require a key**)

Missing key → `IDEMPOTENCY_KEY_REQUIRED`.

Invalid token → `401` (`INVALID_TOKEN` / `UNAUTHORIZED`).

### 1.2 Unified JSON response

On success, read `data`. On failure, read `code` (do not rely on HTTP status alone).

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": {},
  "message": "Success",
  "timestamp": "2026-07-24T22:00:00Z"
}
```

```json
{
  "status": 403,
  "code": "NOT_COURSE_MEMBER",
  "message": "Not a member of this course",
  "timestamp": "2026-07-24T22:00:00Z"
}
```

Preview / download / ZIP responses are **not** this JSON envelope; see [§14](#14-handling-file-streams).

### 1.3 Date and time

| Meaning | Example format |
|------|----------|
| Date | `"2026-01-01"` |
| Time | `"09:00:00"` |
| Date-time | `"2026-07-24T15:14:37"` |

### 1.4 Field name notes

| API | Role field name |
|------|------------|
| `GET /v2/me/courses` | `role` |
| `GET .../members` | `courseRole` |

Use the field returned by each API; do not mix them.

`orderPosition` (weeks, materials): zero-based and increasing.

---

## 2. Typical page flows

### 2.1 Student enters a course

1. `POST /v1/auth/login` → store `accessToken`
2. `GET /v2/me/courses` → course list + `role` per course
3. Open a course: `GET /v2/courses/{id}`
4. In parallel: `GET .../sessions`, `GET .../events`, `GET .../syllabus`, `GET .../weeks` (students only see Published weeks)

> There is **no** student self-enroll API. Enrollment is done by Admin via §10, or by backend ops.

### 2.2 Instructor prepares a course

1. `POST /v2/courses` (requires platform `level=INSTRUCTOR`; creator becomes the course Instructor)
2. `POST .../sessions` (schedule)
3. `POST .../syllabus` (upload PDF)
4. `POST .../weeks` → upload materials → `POST .../weeks/{id}/publish`
5. As needed: `POST .../events`; manage people: `GET .../members`, promote TA

### 2.3 Admin enrolls a student

1. Admin login (`role: "ADMIN"`)
2. `POST /v2/admin/courses/{courseId}/enrollments`, body: `{ "userId": 385 }`
3. Student can then see the course via `GET /v2/me/courses`

---

## 3. Roles and permissions (product rules)

Course roles (within a course): `Instructor` / `TA` / `Student`.  
Use `role` from `GET /v2/me/courses` to show/hide UI actions.

| Capability | Instructor | TA | Student |
|------|:----------:|:--:|:-------:|
| View course / sessions / events / syllabus | ✓ | ✓ | ✓ |
| View weeks (Published only) | ✓ | ✓ | ✓ |
| View weeks (including Draft) | ✓ (platform Admin too) | | |
| Create week | ✓ (course not archived) | | |
| Edit week (rename / reorder / publish / unpublish) | ✓ (course not archived) | | |
| Delete week | ✓ (not archived; week has no materials) | | |
| Upload / create material (file or link) | ✓ (not archived) | ✓ (not archived) | |
| Edit material (rename / reorder / move) | ✓ (not archived) | | |
| Delete own uploaded material | ✓ | ✓ (not archived) | |
| Delete others' material | ✓ (not archived) | | |
| Preview / download material | ✓ (enrolled; students cannot see Draft-week items) | ✓ | ✓ |
| Edit sessions / members / syllabus | ✓ | | |
| Edit events | ✓ | requires `canManageCourseEvents` | |
| Create course | platform `level=INSTRUCTOR` only (then you are that course's Instructor) | | |
| Archive / unarchive / update / delete / transfer Instructor | ✓ (backend-enforced Instructor) | | |
| Deactivate member (Student/TA) | ✓ | | |
| Admin enroll / batch enroll / deactivate enrollment | platform Admin, see §10 | | |
| Course browse `GET /v2/courses` | platform Admin or Instructor (see §5.0) | | |

When course `state=Archived`: writes for syllabus / weeks / materials / sessions / events / TA / enroll return `COURSE_ARCHIVED`. Reads usually still work.

Common errors: `NOT_COURSE_MEMBER`, `NOT_COURSE_INSTRUCTOR`, `ACCESS_DENIED`, `COURSE_ARCHIVED`.

---

## 4. Enum cheat sheet

| Field | Allowed values |
|------|--------|
| `course.state` | `Active` \| `Archived` |
| `role` / `courseRole` | `Instructor` \| `TA` \| `Student` |
| session `type` | `Lecture` \| `Lab` \| `Tutorial` |
| session `dayOfWeek` | `MON` `TUE` `WED` `THU` `FRI` `SAT` `SUN` |
| week `state` | `Draft` \| `Published` |
| material `materialType` | `FILE` \| `LINK` |

Invalid enum → usually `400 BAD_REQUEST`.

---

## 5. Course CRUD

Prefix: `/v2/courses`

### 5.0 Browse courses — `GET /v2/courses`

| | |
|--|--|
| Who can call | Platform **Admin** (all courses); platform `level=INSTRUCTOR` or course Instructor (own courses only) |
| Plain Student | `403 ACCESS_DENIED` |
| Query | Optional: `q` (courseCode/title), `state` (`Active`/`Archived`), `page` (default 0), `size` (default 20, max 100) |

Success `data`:

```json
{
  "items": [ { "id": 9, "courseCode": "DEMO", "title": "...", "state": "Active" } ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

### 5.1 Create course — `POST /v2/courses`

| | |
|--|--|
| Who can call | Platform `level=INSTRUCTOR`; `instructorId` must also be INSTRUCTOR. After create, that `instructorId` becomes the course Instructor |
| Idempotency key | Yes |
| Student creates course | `403 ACCESS_DENIED` |

**Body**

| Field | Type | Required | Notes |
|------|------|------|------|
| courseCode | string | Yes | |
| title | string | Yes | |
| termStartDate | date | Yes | |
| termEndDate | date | Yes | ≥ start |
| instructorId | int | Yes | Usually your own userId |
| tenantId | int | **Yes** | Required; missing → `400 PARAM_MISSING`. Creator and `instructorId` must have `user.tenantId` equal to this value, else `400 TENANT_MISMATCH`; unknown tenant → `404 TENANT_NOT_FOUND`. The frontend currently hard-codes `1` (seed Default tenant); the backend still validates the real `tenantId` |
| description | string | No | |
| location | string | No | |

```json
{
  "tenantId": 1,
  "courseCode": "DEMO-ENROLL",
  "title": "Demo Course With Students",
  "termStartDate": "2026-01-01",
  "termEndDate": "2026-06-30",
  "instructorId": 402,
  "description": "optional"
}
```

Success `data`:

```json
{
  "id": 9,
  "tenantId": 1,
  "courseCode": "DEMO-ENROLL",
  "title": "Demo Course With Students",
  "termStartDate": "2026-01-01",
  "termEndDate": "2026-06-30",
  "description": "optional",
  "location": null,
  "instructorId": 402,
  "state": "Active",
  "archivedAt": null,
  "creatorId": 402,
  "createdAt": "2026-07-24T14:58:32",
  "updatedAt": "2026-07-24T14:58:32"
}
```

Errors: `PARAM_MISSING` (including missing `tenantId`), `BAD_REQUEST`, `USER_NOT_FOUND`, `TENANT_NOT_FOUND`, `TENANT_MISMATCH`.

---

### 5.2 Get course — `GET /v2/courses/{id}`

| | |
|--|--|
| Who can call | Enrolled members; not enrolled → `403 NOT_COURSE_MEMBER`. Cross-tenant (`user.tenantId ≠ course.tenantId`, non-Admin) → `404 COURSE_NOT_FOUND` |

Success `data` shape matches create.

```json
{
  "status": 403,
  "code": "NOT_COURSE_MEMBER",
  "message": "Not a member of this course"
}
```

---

### 5.3 Update course — `PUT /v2/courses/{id}`

| | |
|--|--|
| Who can call | Course Instructor (backend-enforced; others → `NOT_COURSE_INSTRUCTOR`) |
| Idempotency key | Yes |
| Body | All optional: `courseCode` `title` `termStartDate` `termEndDate` `description` `location` |
| Forbidden | **Do not send `instructorId`**; if present → `400 BAD_REQUEST` (instructor transfer is a separate feature) |

Success: `200`, `data` is the updated course.

---

### 5.4 Delete course — `DELETE /v2/courses/{id}`

| | |
|--|--|
| Who can call | Course Instructor (backend-enforced) |

Success: `data` is `null`.  
If the course still has members → `409 CONFLICT`:

```json
{
  "status": 409,
  "code": "CONFLICT",
  "message": "Course cannot be deleted while it still has enrollments"
}
```

---

### 5.5 Archive — `POST /v2/courses/{id}/archive`

| | |
|--|--|
| Who can call | Course Instructor (backend-enforced) |
| Idempotency key | Yes |

Success: `data.state = "Archived"` with `archivedAt`. Calling again when already archived still returns `200`.

---

### 5.6 Unarchive — `POST /v2/courses/{id}/unarchive`

| | |
|--|--|
| Who can call | Course Instructor (backend-enforced) |
| Idempotency key | Yes |

Success: `data.state = "Active"`, `archivedAt = null`. Already Active → still `200`.

---

### 5.7 Transfer Instructor — `POST /v2/courses/{id}/transfer-instructor`

| | |
|--|--|
| Who can call | Current course Instructor |
| Idempotency key | Yes |
| Body | `{ "newInstructorId": 403 }` (must exist with platform `level=INSTRUCTOR`) |
| Archived | `COURSE_ARCHIVED` |

Effects: updates `course.instructorId`; old Instructor → Student; new user becomes the sole Instructor.

---

## 6. Sessions

Prefix: `/v2/courses/{courseId}/sessions`

### 6.1 List — `GET .../sessions`

Enrolled members. Success example:

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": [
    {
      "id": 4,
      "courseId": 9,
      "type": "Lecture",
      "dayOfWeek": "MON",
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "location": "A101",
      "createdAt": "2026-07-24T15:14:37",
      "updatedAt": "2026-07-24T15:14:37"
    }
  ]
}
```

### 6.2 Get one — `GET .../sessions/{sessionId}`

Fetch a single session by id. Enrolled members. Missing → `404 SESSION_NOT_FOUND`.

### 6.3 Create — `POST .../sessions`

Instructor; idempotency key required; archived course → `400 COURSE_ARCHIVED`. All body fields required:

| Field | Constraints |
|------|------|
| type | Lecture / Lab / Tutorial |
| dayOfWeek | MON…SUN |
| startTime / endTime | end **>** start |
| location | string |

```json
{
  "type": "Lecture",
  "dayOfWeek": "MON",
  "startTime": "09:00:00",
  "endTime": "10:30:00",
  "location": "A101"
}
```

Student call → `403 NOT_COURSE_INSTRUCTOR`.

### 6.4 Update — `PUT .../sessions/{sessionId}`

Instructor; idempotency key required; all fields optional; archived course → `COURSE_ARCHIVED`.

### 6.5 Delete — `DELETE .../sessions/{sessionId}`

Instructor; archived course → `COURSE_ARCHIVED`. Success `data: null`; subsequent GET → `SESSION_NOT_FOUND`.

---

## 7. Events

Prefix: `/v2/courses/{courseId}/events`  
Writes: Instructor, or TA with `canManageCourseEvents=true`.

### 7.1 List — `GET .../events`

Enrolled members. Success `data` example:

```json
[
  {
    "id": 2,
    "courseId": 9,
    "name": "Midterm Exam",
    "date": "2026-03-15",
    "startTime": "14:00:00",
    "endTime": "16:00:00",
    "location": "Hall B",
    "description": "Bring ID",
    "createdAt": "2026-07-24T15:00:00",
    "updatedAt": "2026-07-24T15:00:00"
  }
]
```

### 7.2 Get one — `GET .../events/{eventId}`

Fetch a single event by id. Missing → `404 COURSE_EVENT_NOT_FOUND`.

### 7.3 Create — `POST .../events`

Idempotency key required; archived course → `400 COURSE_ARCHIVED`.

| Field | Required |
|------|------|
| name, date, startTime, endTime | Yes (end > start) |
| location, description | No |

No permission → `403 ACCESS_DENIED`.

### 7.4 Update — `PUT .../events/{eventId}`

| | |
|--|--|
| Who can call | Instructor, or TA with `canManageCourseEvents=true` |
| Idempotency key | Yes |
| Success | `200`, `data` is the updated event |
| Common errors | `403 ACCESS_DENIED`; `400 COURSE_ARCHIVED`; `404 COURSE_EVENT_NOT_FOUND`; invalid time `400 BAD_REQUEST` |

**Body (all optional; only sent fields are updated)**

| Field | Type | Notes |
|------|------|------|
| name | string | Empty string → `BAD_REQUEST` |
| date | date | `"2026-03-15"` |
| startTime | time | `"14:00:00"` |
| endTime | time | After merge must be **>** startTime |
| location | string | `""` clears to `null` |
| description | string | `""` clears to `null` |

Request example (name + description only):

```json
{
  "name": "Midterm Exam Updated",
  "description": "Bring student ID"
}
```

Success `data` shape matches a list item (includes `id`, `courseId`, times/location, `updatedAt`, etc.).

### 7.5 Delete — `DELETE .../events/{eventId}`

| | |
|--|--|
| Who can call | Instructor, or TA with `canManageCourseEvents=true` |
| Idempotency key | No |
| Success | `200`, `data` is `null` |
| Common errors | `403 ACCESS_DENIED`; `400 COURSE_ARCHIVED`; missing → `404 COURSE_EVENT_NOT_FOUND` |

After delete, `GET .../events/{eventId}` → `404 COURSE_EVENT_NOT_FOUND`.

---

## 8. Members / TA

Prefix: `/v2/courses/{courseId}/members`  
Cannot change TA when the course is archived.

### 8.1 Member list — `GET .../members`

Instructor only. Success item example:

```json
{
  "id": 6,
  "courseId": 9,
  "userId": 385,
  "userName": "Alex Rivera",
  "userEmail": "regtest1@example.com",
  "courseRole": "Student",
  "canGrade": false,
  "canPostAnnouncements": false,
  "canManageGroups": false,
  "canManageCourseEvents": false,
  "active": true,
  "enrolledAt": "2026-07-24T14:58:32",
  "createdAt": "2026-07-24T14:58:32",
  "updatedAt": "2026-07-24T14:58:32"
}
```

### 8.2 Promote to TA — `POST .../members/{userId}/ta`

Promote a course **Student** to **TA**, optionally setting TA permission flags.

| | |
|--|--|
| Method & path | `POST /v2/courses/{courseId}/members/{userId}/ta` |
| Who can call | Instructor only |
| Idempotency key | Yes |
| Path | `courseId`: course id; `userId`: target userId (must already be a course member) |
| Course state | Archived → `400 COURSE_ARCHIVED` |

**Preconditions (target user)**

- Already in this course Enrollment with `active=true`
- Current `courseRole` must be `Student` (already TA / Instructor cannot be promoted again)

**Body (optional)**

Body may be omitted or `{}`: all four permissions default to `false`.  
Only an explicit `true` turns a permission on (omitted / `false` / `null` = off).

| Field | Type | Default | Meaning (frontend capability flags) |
|------|------|------|------------------------|
| canGrade | boolean | false | Can grade (used by assignment modules, etc.) |
| canPostAnnouncements | boolean | false | Can post announcements |
| canManageGroups | boolean | false | Can manage groups |
| canManageCourseEvents | boolean | false | Can create/update/delete course Events |

```json
{
  "canGrade": true,
  "canPostAnnouncements": false,
  "canManageGroups": false,
  "canManageCourseEvents": true
}
```

**Success `200`, `data` shape**

```json
{
  "member": {
    "id": 8,
    "courseId": 9,
    "userId": 387,
    "userName": "Casey Morgan",
    "userEmail": "regtest3@example.com",
    "courseRole": "TA",
    "canGrade": true,
    "canPostAnnouncements": false,
    "canManageGroups": false,
    "canManageCourseEvents": true,
    "active": true,
    "enrolledAt": "2026-07-24T14:58:32",
    "createdAt": "2026-07-24T14:58:32",
    "updatedAt": "2026-07-24T15:00:00"
  },
  "warnings": []
}
```

- `member`: member after promotion (`courseRole` becomes `TA`, permissions persisted)
- `warnings`: string array; currently usually `[]`; frontend may reserve UI for it

**Common errors**

| code | When |
|------|------|
| `NOT_COURSE_INSTRUCTOR` | Caller is not this course's Instructor |
| `COURSE_ARCHIVED` | Course is archived |
| `ENROLLMENT_NOT_FOUND` | Target user is not in the course |
| `ENROLLMENT_NOT_ACTIVE` | Target enrollment is inactive |
| `INVALID_ROLE_TRANSITION` | Target is not Student (e.g. already TA) |
| `COURSE_NOT_FOUND` | Course does not exist |

### 8.3 Revoke TA — `DELETE .../members/{userId}/ta`

Target must be TA. On success, `courseRole` becomes `Student` again.

### 8.4 Update TA permissions — `PATCH .../members/{userId}/ta/permissions`

Idempotency key required; body is the four booleans above.

### 8.5 Deactivate member — `DELETE .../members/{userId}`

| | |
|--|--|
| Who can call | Instructor |
| Idempotency key | Yes |
| Semantics | Soft deactivate: `active=false`; Student/TA only |
| Deactivate Instructor | `403 ACCESS_DENIED` |
| Already inactive | `200` (idempotent) |
| Archived | `COURSE_ARCHIVED` |

After deactivate, the user no longer sees the course in `GET /v2/me/courses`.

---

## 9. My courses

### `GET /v2/me/courses`

Authenticated user. Returns courses with an **active** enrollment (the course itself may be Archived), each with `role`.

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": [
    {
      "id": 9,
      "tenantId": 1,
      "courseCode": "DEMO-ENROLL",
      "title": "Demo Course With Students",
      "termStartDate": "2026-01-01",
      "termEndDate": "2026-06-30",
      "description": "Created to inspect enroll responses",
      "location": null,
      "instructorId": 402,
      "state": "Active",
      "archivedAt": null,
      "role": "Student"
    }
  ]
}
```

Use `role` here for navigation: student timetable vs instructor prep entry.

---

## 10. Admin enroll

### 10.1 Single enroll — `POST /v2/admin/courses/{courseId}/enrollments`

| | |
|--|--|
| Who can call | Platform Admin |
| Idempotency key | Yes |
| Body | `{ "userId": 385 }` |

Success: member object with `courseRole: "Student"`.  
Non-Admin → `ACCESS_DENIED`; already active → `CONFLICT`; archived → `COURSE_ARCHIVED`.

### 10.2 Batch / email enroll — `POST /v2/admin/courses/{courseId}/enrollments/batch`

| | |
|--|--|
| Who can call | Platform Admin |
| Idempotency key | Yes |
| Body | `{ "userIds": [385], "emails": ["regtest2@example.com"] }` (at least one) |

HTTP is always **200** (including partial success):

```json
{
  "succeeded": [ { "userId": 385, "courseRole": "Student", "active": true } ],
  "failed": [ { "email": "missing@example.com", "code": "USER_NOT_FOUND", "message": "User not found" } ]
}
```

Emails are resolved to users; missing emails go to `failed`; active conflict → `CONFLICT`; inactive → reactivated as success.

### 10.3 Deactivate enrollment — `DELETE /v2/admin/courses/{courseId}/enrollments/{userId}`

| | |
|--|--|
| Who can call | Platform Admin |
| Idempotency key | Yes |
| Semantics | Soft deactivate `active=false` |
| Sole active Instructor | `409 CONFLICT` |
| Already inactive | `200` |

---

## 11. Syllabus

Prefix: `/v2/courses/{courseId}/syllabus`  
Upload is PDF-only; archive blocks upload/restore.

### 11.1 Metadata — `GET .../syllabus`

Enrolled members. Not uploaded yet:

```json
{ "posted": false }
```

Uploaded (Instructor may also get `canRestorePrevious`):

```json
{
  "posted": true,
  "versionId": 1,
  "originalFilename": "syllabus.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "uploadedBy": 402,
  "uploadedAt": "2026-07-24T15:00:00",
  "canRestorePrevious": false
}
```

### 11.2 Preview / download

Returns the **current version** syllabus PDF as a file stream (not `{ status, code, data }` JSON). Call §11.1 first and only request when `posted === true`. See [§14](#14-handling-file-streams).

#### Preview — `GET /v2/courses/{courseId}/syllabus/preview`

| | |
|--|--|
| Who can call | Enrolled members (Instructor / TA / Student); Admin may bypass enrollment |
| Idempotency key | No |
| Success | PDF binary stream |
| Content-Type | Usually `application/pdf` |
| Content-Disposition | `inline; filename="original-name.pdf"` (inline browser view) |

Frontend tip: with Bearer, use `fetch` → `blob` → `URL.createObjectURL`, open in `<iframe>` / new window; or `window.open(url)` when same-origin cookies apply (this project is Bearer-first; prefer blob).

#### Download — `GET /v2/courses/{courseId}/syllabus/download`

| | |
|--|--|
| Who can call | Same (enrolled / Admin) |
| Idempotency key | No |
| Success | PDF binary stream |
| Content-Type | Usually `application/pdf` |
| Content-Disposition | `attachment; filename="original-name.pdf"` (save-as) |

Frontend tip: `fetch` + Bearer → `blob` → create `<a download={originalFilename}>` and click; take `originalFilename` from §11.1 `data.originalFilename`.

#### Common errors

| code | When |
|------|------|
| `NOT_COURSE_MEMBER` | Not enrolled (non-Admin) |
| `SYLLABUS_NOT_FOUND` | Never uploaded, or current version file read failed |
| `COURSE_NOT_FOUND` | Course does not exist |
| `UNAUTHORIZED` / `INVALID_TOKEN` | Not logged in or token invalid |

Errors may still be JSON (`Content-Type: application/json`); check content type before parsing as JSON vs file.

### 11.3 Upload — `POST .../syllabus`

Upload (or overwrite) the current syllabus PDF. On re-upload: new file becomes current; previous current becomes previous (for §11.4 restore).

| | |
|--|--|
| Method & path | `POST /v2/courses/{courseId}/syllabus` |
| Who can call | Instructor only |
| Idempotency key | Yes |
| Content-Type | `multipart/form-data` (do not set manually on FormData; browser adds boundary) |
| Course state | Archived → `400 COURSE_ARCHIVED` |

**Form fields**

| Name | Required | Notes |
|--------|------|------|
| `file` | Yes | **Single** PDF (`@RequestPart("file")`) |

Constraints:

- PDF only (`.pdf` extension; Content-Type usually `application/pdf`)
- Default max size **200MB** (`lms.content.max-file-bytes`)
- Empty file not allowed

**FormData example**

```js
const fd = new FormData();
fd.append("file", pdfFile); // field name must be file
await fetch(`${BASE}/v2/courses/${courseId}/syllabus`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Idempotency-Key": crypto.randomUUID(),
  },
  body: fd,
});
```

**Success `200`, `data`** (Instructor view; same shape as §11.1 uploaded):

```json
{
  "posted": true,
  "versionId": 2,
  "originalFilename": "syllabus-v2.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "uploadedBy": 402,
  "uploadedAt": "2026-07-24T16:00:00",
  "canRestorePrevious": true
}
```

After the second and later successful uploads, `canRestorePrevious` is usually `true` (a previous version exists).

**Common errors**

| code | When |
|------|------|
| `NOT_COURSE_INSTRUCTOR` | Not this course's Instructor |
| `COURSE_ARCHIVED` | Course is archived |
| `UNSUPPORTED_FILE_TYPE` | Not a PDF |
| `FILE_TOO_LARGE` | Over size limit |
| `BAD_REQUEST` / `PARAM_MISSING` | Missing `file` or empty file |
| `INTERNAL_SERVER_ERROR` | Storage upload failed |
| `IDEMPOTENCY_KEY_REQUIRED` | Missing idempotency key |

### 11.4 Restore previous — `POST .../syllabus/restore`

Instructor; idempotency key required. No current syllabus → `SYLLABUS_NOT_FOUND`; no previous → `NO_PREVIOUS_SYLLABUS_VERSION`.

### 11.5 Clear syllabus — `DELETE .../syllabus`

| | |
|--|--|
| Who can call | Instructor |
| Idempotency key | Yes |
| Archived | `COURSE_ARCHIVED` |

Clears current/previous pointers (`posted: false`); version rows remain in DB but are not exposed. Clearing again → `200`.

---

## 12. Weeks

Prefix: `/v2/courses/{courseId}/weeks`  
New weeks default to `Draft`. Writes: Instructor and course not archived.

### 12.1 List — `GET .../weeks`

- **Instructor / Admin**: Draft + Published (all visible)  
- **TA / Student**: Published only (Draft weeks do not appear; TA same as student)

Success item shape:

```json
{
  "id": 1,
  "courseId": 9,
  "title": "Week 1",
  "orderPosition": 0,
  "state": "Published",
  "createdAt": "2026-07-24T15:00:00",
  "updatedAt": "2026-07-24T15:00:00",
  "materials": [
    {
      "id": 10,
      "weekId": 1,
      "courseId": 9,
      "materialType": "FILE",
      "displayName": "slides.pdf",
      "orderPosition": 0,
      "originalFilename": "slides.pdf",
      "contentType": "application/pdf",
      "extension": "pdf",
      "sizeBytes": 204800,
      "linkUrl": null,
      "uploadedBy": 402,
      "previewAvailable": true,
      "downloadUrl": "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials/10/download",
      "previewUrl": "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials/10/preview",
      "createdAt": "2026-07-24T15:01:00",
      "updatedAt": "2026-07-24T15:01:00"
    }
  ]
}
```

`downloadUrl` / `previewUrl`: relative or absolute API paths; **still require Bearer** (not an anonymous CDN).

### 12.2 Create — `POST .../weeks`

Append a week at the end of the course; unpublished by default.

| | |
|--|--|
| Method & path | `POST /v2/courses/{courseId}/weeks` |
| Who can call | Instructor only (TA / Student → `403 NOT_COURSE_INSTRUCTOR`) |
| Idempotency key | Yes |
| Course state | Archived → `400 COURSE_ARCHIVED` |

**Body**

| Field | Type | Required | Notes |
|------|------|------|------|
| title | string | Yes | Non-empty after trim; max **255** chars |

```json
{ "title": "Week 1" }
```

**Success `200`, `data` example**

```json
{
  "id": 3,
  "courseId": 9,
  "title": "Week 1",
  "orderPosition": 2,
  "state": "Draft",
  "createdAt": "2026-07-24T16:00:00",
  "updatedAt": "2026-07-24T16:00:00",
  "materials": []
}
```

- `state` is always `Draft` (hidden from student/TA list until publish)
- `orderPosition`: after current max (from 0)

**Common errors**: `NOT_COURSE_INSTRUCTOR`, `COURSE_ARCHIVED`, `PARAM_MISSING` (missing title), `BAD_REQUEST` (title > 255)

---

### 12.3 Rename — `PATCH .../weeks/{weekId}`

Updates title only; does not change `state` / `orderPosition`.

| | |
|--|--|
| Method & path | `PATCH /v2/courses/{courseId}/weeks/{weekId}` |
| Who can call | Instructor only |
| Idempotency key | Yes |
| Course state | Archived → `COURSE_ARCHIVED` |

**Body**

| Field | Required | Notes |
|------|------|------|
| title | Yes | Same as create: non-empty, ≤255 |

```json
{ "title": "Week 1 — Intro" }
```

Success: `200`, `data` is the updated week (including `materials`).  
Week missing or not in course → `404 WEEK_NOT_FOUND`.

---

### 12.4 Reorder — `PUT .../weeks/reorder`

Rewrite `orderPosition` for all weeks from the given order (0, 1, 2, …).

| | |
|--|--|
| Method & path | `PUT /v2/courses/{courseId}/weeks/reorder` |
| Who can call | Instructor only |
| Idempotency key | Yes |
| Course state | Archived → `COURSE_ARCHIVED` |

**Body**

| Field | Required | Notes |
|------|------|------|
| weekIds | Yes | A permutation of **all** week ids for this course (may include Draft + Published) |

Rules:

- Id set must match the current list **exactly** (no missing, extra, duplicates, or foreign course ids)
- Array order = display order: `weekIds[0]` → `orderPosition=0`, and so on

```json
{ "weekIds": [3, 1, 2] }
```

Success: `200`, `data` is the reordered **week array**.

**Common errors**

| code | When |
|------|------|
| `PARAM_MISSING` | `weekIds` missing |
| `BAD_REQUEST` | Id set does not match current weeks |
| `NOT_COURSE_INSTRUCTOR` | Not Instructor |
| `COURSE_ARCHIVED` | Course archived |

Frontend drag-and-drop: `GET .../weeks` for all ids → submit full `weekIds` in new order.

---

### 12.5 Publish / unpublish

- `POST .../weeks/{weekId}/publish`
- `POST .../weeks/{weekId}/unpublish`

Both require an idempotency key.

### 12.6 Delete — `DELETE .../weeks/{weekId}`

If the week still has materials → `409 WEEK_NOT_EMPTY` (delete materials first).

### 12.7 Download week ZIP — `GET .../weeks/{weekId}/download.zip`

Enrolled members; student access to unpublished weeks behaves as not found (`WEEK_NOT_FOUND`).  
Needs at least one FILE material, otherwise `BAD_REQUEST`. See §14.

---

## 13. Materials

Prefix: `/v2/courses/{courseId}/weeks/{weekId}/materials`

- **Upload / delete own**: Instructor or TA (course not archived)
- **Rename / reorder / move / delete others'**: Instructor only (course not archived)

### 13.1 Create — `POST .../materials`

| | |
|--|--|
| Who can call | Instructor or TA (course not archived) |
| Idempotency key | No |

`multipart/form-data`:

| Field | Notes |
|------|------|
| `files` | Files; multiple allowed (repeat same name / array) |
| `linkUrl` | Optional external link |
| `linkDisplayName` | Optional link display name |
| | At least one of `files` or `linkUrl` |

#### Example 1: files only (JS)

```js
const fd = new FormData();
fd.append("files", pdfFile); // field name must be files; append multiple times for multiple files

const res = await fetch(
  "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials",
  {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` }, // do not set Content-Type
    body: fd,
  }
);
const json = await res.json();
// json.data = Material array
```

#### Example 2: link only (JS)

```js
const fd = new FormData();
fd.append("linkUrl", "https://example.com/reading");
fd.append("linkDisplayName", "Week 1 Reading");

await fetch("https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials", {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` },
  body: fd,
});
```

#### Example 3: curl (file + link in one request)

```bash
curl -X POST "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials" \
  -H "Authorization: Bearer <accessToken>" \
  -F "files=@./slides.pdf" \
  -F "linkUrl=https://example.com/reading" \
  -F "linkDisplayName=Week 1 Reading"
```

#### Success response example (`200`)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": [
    {
      "id": 10,
      "weekId": 1,
      "courseId": 9,
      "materialType": "FILE",
      "displayName": "slides",
      "orderPosition": 0,
      "originalFilename": "slides.pdf",
      "contentType": "application/pdf",
      "extension": "pdf",
      "sizeBytes": 204800,
      "linkUrl": null,
      "uploadedBy": 402,
      "previewAvailable": true,
      "downloadUrl": "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials/10/download",
      "previewUrl": "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials/10/preview",
      "createdAt": "2026-07-24T15:01:00",
      "updatedAt": "2026-07-24T15:01:00"
    },
    {
      "id": 11,
      "weekId": 1,
      "courseId": 9,
      "materialType": "LINK",
      "displayName": "Week 1 Reading",
      "orderPosition": 1,
      "originalFilename": null,
      "contentType": null,
      "extension": null,
      "sizeBytes": null,
      "linkUrl": "https://example.com/reading",
      "uploadedBy": 402,
      "previewAvailable": false,
      "downloadUrl": "https://dev.xlearnedu.com:8080/api/v2/courses/9/weeks/1/materials/11/download",
      "previewUrl": null,
      "createdAt": "2026-07-24T15:01:00",
      "updatedAt": "2026-07-24T15:01:00"
    }
  ],
  "message": "Success",
  "timestamp": "2026-07-24T22:00:00Z"
}
```

### 13.2 Rename — `PATCH .../materials/{materialId}`

Instructor only; idempotency key required. `{ "displayName": "..." }`.

### 13.3 Reorder — `PUT .../materials/reorder`

Instructor only; idempotency key required. `materialIds` must be a permutation of **all** material ids in that week.

### 13.4 Move — `POST .../materials/{materialId}/move`

Instructor only; idempotency key required. `{ "targetWeekId": 12 }`.

### 13.5 Delete — `DELETE .../materials/{materialId}`

| | |
|--|--|
| Who can call | Instructor: any; TA: only when `uploadedBy` is self; otherwise `403 ACCESS_DENIED` |

### 13.6 Preview / download

- `GET .../materials/{materialId}/preview` — previewable types only (see `previewAvailable`)
- `GET .../materials/{materialId}/download` — FILE streams the file; **LINK may 302 to the external URL**

See §14. Not previewable → `BAD_REQUEST`.

---

## 14. Handling file streams

Applies to: syllabus preview/download, material preview/download, week `download.zip`.

| Point | Notes |
|----|------|
| Auth | Same as JSON APIs: `Authorization: Bearer` |
| Success body | Binary stream, **not** `{ status, code, data }` |
| Preview | `Content-Disposition: inline` → `blob` + `URL.createObjectURL` or new window |
| Download | `attachment` → `blob` then `<a download>`, or request returned `downloadUrl` same-origin |
| LINK download | May **302 redirect** off-site; watch `fetch` `redirect`; or `window.open(downloadUrl)` for external links |
| Errors | May still return JSON (e.g. `SYLLABUS_NOT_FOUND`); check `Content-Type` first |

```js
// Download with token
const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
if (!res.ok) {
  const err = await res.json(); // may be error JSON
  throw err;
}
const blob = await res.blob();
const a = document.createElement("a");
a.href = URL.createObjectURL(blob);
a.download = filenameHint || "file";
a.click();
```

---

## 15. Endpoint cheat sheet

| Method | Path | Who | Idempotency key |
|------|------|------|----------|
| POST | `/v2/courses` | level=INSTRUCTOR | Yes |
| GET | `/v2/courses` | Admin / Instructor | |
| GET | `/v2/courses/{id}` | Enrolled | |
| PUT | `/v2/courses/{id}` | Instructor | Yes |
| DELETE | `/v2/courses/{id}` | Instructor | |
| POST | `/v2/courses/{id}/archive` | Instructor | Yes |
| POST | `/v2/courses/{id}/unarchive` | Instructor | Yes |
| POST | `/v2/courses/{id}/transfer-instructor` | Instructor | Yes |
| GET | `/v2/courses/{id}/sessions` | Enrolled | |
| POST/PUT/DELETE | `.../sessions` | Instructor | POST/PUT |
| GET | `/v2/courses/{id}/events` | Enrolled | |
| POST/PUT/DELETE | `.../events` | Instructor / event TA | POST/PUT |
| GET | `/v2/courses/{id}/members` | Instructor | |
| POST/DELETE/PATCH | `.../members/.../ta` | Instructor | POST/PATCH |
| DELETE | `.../members/{userId}` | Instructor (deactivate Student/TA) | Yes |
| GET | `/v2/me/courses` | Authenticated | |
| POST | `/v2/admin/courses/{id}/enrollments` | Admin | Yes |
| POST | `.../enrollments/batch` | Admin | Yes |
| DELETE | `.../enrollments/{userId}` | Admin | Yes |
| GET/POST/DELETE | `.../syllabus` | Read: members; write/delete: Instructor | Write/delete: yes |
| GET/POST/... | `.../weeks` | Read: members; write: Instructor | Most writes: yes |
| POST | `.../materials` | Instructor / TA upload | No |
| DELETE | `.../materials/{id}` | Instructor; TA own only | |
| PATCH/PUT/POST move | `.../materials...` | Instructor only | Yes |

---

## 16. Local test accounts

| Use | email | password |
|------|-------|----------|
| Student | `regtest1@example.com` … `regtest5@example.com` | `Test12345` |
| Instructor | `teachtest2@example.com` | `Test12345` |

```http
POST /v1/auth/login
Content-Type: application/json

{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "role": "USER"
}
```

Take `data.accessToken` from the response → `Authorization: Bearer ...`.
