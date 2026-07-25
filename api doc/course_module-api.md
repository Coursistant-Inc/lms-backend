# Course 模块 API 参考（前端）

给前端联调 / 写页面用。  
Base URL：`http://localhost:8080/api`

---

## 目录

1. [怎么调用](#1-怎么调用)
2. [典型页面流程](#2-典型页面流程)
3. [角色与权限（产品约定）](#3-角色与权限产品约定)
4. [枚举速查](#4-枚举速查)
5. [Course CRUD](#5-course-crud)
6. [Sessions](#6-sessions课时表)
7. [Events](#7-events课程事件)
8. [Members / TA](#8-members--ta)
9. [My courses](#9-my-courses)
10. [Admin enroll](#10-admin-enroll)
11. [Syllabus](#11-syllabus)
12. [Weeks](#12-weeks)
13. [Materials](#13-materials)
14. [文件流怎么接](#14-文件流怎么接)
15. [端点速查表](#15-端点速查表)
16. [本地测试账号](#16-本地测试账号)

---

## 1. 怎么调用

### 1.1 Header

| Header | 何时需要 |
|--------|----------|
| `Authorization: Bearer {accessToken}` | 几乎所有接口（先 `POST /v1/auth/login`） |
| `Idempotency-Key: {uuid}` | **仅下列写接口**（每次请求用新 UUID） |
| `Content-Type: application/json` | JSON body |
| `Content-Type: multipart/form-data` | 上传文件（浏览器 FormData 会自动带） |

**需要 `Idempotency-Key` 的写接口：**

- `POST/PUT /v2/courses`、`POST /v2/courses/{id}/archive`
- `POST/PUT .../sessions`
- `POST/PUT .../events`
- `POST/PATCH .../members/.../ta...`
- `POST /v2/admin/courses/{id}/enrollments`
- `POST .../syllabus`、`POST .../syllabus/restore`
- `POST/PATCH/PUT .../weeks...`（含 publish/unpublish/reorder；不含 DELETE）
- `PATCH/PUT/POST .../materials` 的 rename / reorder / move（**创建 materials 不需要**）

缺 Key → `IDEMPOTENCY_KEY_REQUIRED`。

Token 无效 → `401`（`INVALID_TOKEN` / `UNAUTHORIZED`）。

### 1.2 统一 JSON 响应

成功看 `data`；失败看 `code`（不要只看 HTTP status）。

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

预览 / 下载 / ZIP **不是**这层 JSON，见 [§14](#14-文件流怎么接)。

### 1.3 日期时间

| 含义 | 格式示例 |
|------|----------|
| 日期 | `"2026-01-01"` |
| 时间 | `"09:00:00"` |
| 日期时间 | `"2026-07-24T15:14:37"` |

### 1.4 字段名注意

| 接口 | 角色字段名 |
|------|------------|
| `GET /v2/me/courses` | `role` |
| `GET .../members` | `courseRole` |

前端按接口返回字段用，不要混用。

`orderPosition`（周、资料）：从 **0** 开始递增。

---

## 2. 典型页面流程

### 2.1 学生进课

1. `POST /v1/auth/login` → 存 `accessToken`
2. `GET /v2/me/courses` → 课列表 + 每门课的 `role`
3. 进某课：`GET /v2/courses/{id}`
4. 并行：`GET .../sessions`、`GET .../events`、`GET .../syllabus`、`GET .../weeks`（学生只看到已发布周）

> 学生端**没有**自助加入课程的 API；入课由 Admin 调 §10，或后台处理。

### 2.2 教师备课

1. `POST /v2/courses`（需 `level=INSTRUCTOR`；建完自己成为该课 Instructor）
2. `POST .../sessions`（排课）
3. `POST .../syllabus`（上传 PDF）
4. `POST .../weeks` → 上传 materials → `POST .../weeks/{id}/publish`
5. 需要时：`POST .../events`；管人：`GET .../members`、提升 TA

### 2.3 管理员给学生入课

1. Admin 登录（`role: "ADMIN"`）
2. `POST /v2/admin/courses/{courseId}/enrollments`，body：`{ "userId": 385 }`
3. 学生再 `GET /v2/me/courses` 即可看到该课

---

## 3. 角色与权限（产品约定）

课程角色（在某门课里）：`Instructor` / `TA` / `Student`。  
用 `GET /v2/me/courses` 的 `role` 控制按钮显隐。

| 能力 | Instructor | TA | Student |
|------|:----------:|:--:|:-------:|
| 看课详情 / sessions / events / syllabus | ✓ | ✓ | ✓ |
| 看 weeks（仅 Published） | ✓ | ✓ | ✓ |
| 看 weeks（含 Draft） | ✓（平台 Admin 也可） | | |
| 创建 week | ✓（课未归档） | | |
| 修改 week（重命名 / 重排 / 发布 / 取消发布） | ✓（课未归档） | | |
| 删除 week | ✓（课未归档；周内无 materials） | | |
| 上传 / 创建 material（文件或链接） | ✓（课未归档） | ✓（课未归档） | |
| 修改 material（重命名 / 重排 / 移动到其他周） | ✓（课未归档） | | |
| 删除自己上传的 material | ✓ | ✓（课未归档） | |
| 删除他人上传的 material | ✓（课未归档） | | |
| 预览 / 下载 material | ✓（已入课；学生看不到未发布周里的） | ✓ | ✓ |
| 改 sessions / members / syllabus | ✓ | | |
| 改 events | ✓ | 需 `canManageCourseEvents` | |
| 建课 | 仅平台 `level=INSTRUCTOR`（建完自己是该课 Instructor） | | |
| 归档 / 更新 / 删除课 | ✓（前端按 Instructor 控按钮） | | |
| Admin 入课 | 平台 Admin，见 §10 | | |

课 `state=Archived` 时：大纲 / 周次 / 资料 / TA / 入课等写操作会 `COURSE_ARCHIVED`。读一般仍可。  
（Events / Sessions 写目前后端仍可能成功；**产品上归档后请隐藏写入口**。）

常见错误：`NOT_COURSE_MEMBER`、`NOT_COURSE_INSTRUCTOR`、`ACCESS_DENIED`、`COURSE_ARCHIVED`。

---

## 4. 枚举速查

| 字段 | 合法值 |
|------|--------|
| `course.state` | `Active` \| `Archived` |
| `role` / `courseRole` | `Instructor` \| `TA` \| `Student` |
| session `type` | `Lecture` \| `Lab` \| `Tutorial` |
| session `dayOfWeek` | `MON` `TUE` `WED` `THU` `FRI` `SAT` `SUN` |
| week `state` | `Draft` \| `Published` |
| material `materialType` | `FILE` \| `LINK` |

非法枚举 → 多为 `400 BAD_REQUEST`。

---

## 5. Course CRUD

前缀：`/v2/courses`

### 5.1 创建课程 — `POST /v2/courses`

| | |
|--|--|
| 谁可以调 | 平台 `level=INSTRUCTOR`；`instructorId` 也必须是 INSTRUCTOR。建完该 `instructorId` 成为课程 Instructor |
| 需要幂等 Key | 是 |
| 学生建课 | `403 ACCESS_DENIED` |

**Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| courseCode | string | 是 | |
| title | string | 是 | |
| termStartDate | date | 是 | |
| termEndDate | date | 是 | ≥ start |
| instructorId | int | 是 | 通常填自己的 userId |
| description | string | 否 | |
| location | string | 否 | |
| tenantId | int | 否 | |

```json
{
  "courseCode": "DEMO-ENROLL",
  "title": "Demo Course With Students",
  "termStartDate": "2026-01-01",
  "termEndDate": "2026-06-30",
  "instructorId": 402,
  "description": "optional"
}
```

成功 `data`：

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

错误：`PARAM_MISSING`、`BAD_REQUEST`、`USER_NOT_FOUND`。

---

### 5.2 获取课程 — `GET /v2/courses/{id}`

| | |
|--|--|
| 谁可以调 | 已入课成员；未入课 → `403 NOT_COURSE_MEMBER` |

成功 `data` 形状同创建。

```json
{
  "status": 403,
  "code": "NOT_COURSE_MEMBER",
  "message": "Not a member of this course"
}
```

---

### 5.3 更新课程 — `PUT /v2/courses/{id}`

| | |
|--|--|
| 谁可以调 | Instructor（前端控按钮） |
| 需要幂等 Key | 是 |
| Body | 字段均可选：`courseCode` `title` `termStartDate` `termEndDate` `description` `location` `instructorId` |

成功：`200`，`data` 为更新后课程。

---

### 5.4 删除课程 — `DELETE /v2/courses/{id}`

| | |
|--|--|
| 谁可以调 | Instructor（前端控按钮） |

成功：`data` 为 `null`。  
课上还有成员 → `409 CONFLICT`：

```json
{
  "status": 409,
  "code": "CONFLICT",
  "message": "Course cannot be deleted while it still has enrollments"
}
```

---

### 5.5 归档 — `POST /v2/courses/{id}/archive`

| | |
|--|--|
| 谁可以调 | Instructor（前端控按钮） |
| 需要幂等 Key | 是 |

成功：`data.state = "Archived"`，带 `archivedAt`。已归档再调仍 `200`。

---

## 6. Sessions（课时表）

前缀：`/v2/courses/{courseId}/sessions`

### 6.1 列表 — `GET .../sessions`

已入课成员。成功示例：

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

### 6.2 单个 — `GET .../sessions/{sessionId}`

按 id 查单条。已入课。不存在 → `404 SESSION_NOT_FOUND`。

### 6.3 创建 — `POST .../sessions`

Instructor；需要幂等 Key。Body 均必填：

| 字段 | 约束 |
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

学生调 → `403 NOT_COURSE_INSTRUCTOR`。

### 6.4 更新 — `PUT .../sessions/{sessionId}`

Instructor；需要幂等 Key；字段均可选。

### 6.5 删除 — `DELETE .../sessions/{sessionId}`

Instructor。成功 `data: null`；再 GET → `SESSION_NOT_FOUND`。

---

## 7. Events（课程事件）

前缀：`/v2/courses/{courseId}/events`  
写：Instructor，或 `canManageCourseEvents=true` 的 TA。

### 7.1 列表 — `GET .../events`

已入课。成功 `data` 示例：

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

### 7.2 单个 — `GET .../events/{eventId}`

按 id 查单条。不存在 → `404 COURSE_EVENT_NOT_FOUND`。

### 7.3 创建 — `POST .../events`

需要幂等 Key。

| 字段 | 必填 |
|------|------|
| name, date, startTime, endTime | 是（end > start） |
| location, description | 否 |

无权限 → `403 ACCESS_DENIED`。

### 7.4 更新 — `PUT .../events/{eventId}`

| | |
|--|--|
| 谁可以调 | Instructor，或 `canManageCourseEvents=true` 的 TA |
| 需要幂等 Key | 是 |
| 成功 | `200`，`data` 为更新后的单条 event |
| 常见错误 | `403 ACCESS_DENIED`；`404 COURSE_EVENT_NOT_FOUND`；时间非法 `400 BAD_REQUEST` |

**Body（字段均可选，传什么改什么）**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | 不能传空串（会 `BAD_REQUEST`） |
| date | date | `"2026-03-15"` |
| startTime | time | `"14:00:00"` |
| endTime | time | 合并后须 **>** startTime |
| location | string | 传 `""` 会清成 `null` |
| description | string | 传 `""` 会清成 `null` |

请求示例（只改名称和说明）：

```json
{
  "name": "Midterm Exam Updated",
  "description": "Bring student ID"
}
```

成功 `data` 形状同列表单条（含 `id`、`courseId`、时间地点、`updatedAt` 等）。

### 7.5 删除 — `DELETE .../events/{eventId}`

| | |
|--|--|
| 谁可以调 | Instructor，或 `canManageCourseEvents=true` 的 TA |
| 需要幂等 Key | 否 |
| 成功 | `200`，`data` 为 `null` |
| 常见错误 | `403 ACCESS_DENIED`；不存在 → `404 COURSE_EVENT_NOT_FOUND` |

删除后再 `GET .../events/{eventId}` → `404 COURSE_EVENT_NOT_FOUND`。

---

## 8. Members / TA

前缀：`/v2/courses/{courseId}/members`  
课已归档时不能改 TA。

### 8.1 成员列表 — `GET .../members`

仅 Instructor。成功单条示例：

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

### 8.2 提升 TA — `POST .../members/{userId}/ta`

把该课里的 **Student** 提升为 **TA**，并可顺带设置 TA 权限开关。

| | |
|--|--|
| 方法与路径 | `POST /v2/courses/{courseId}/members/{userId}/ta` |
| 谁可以调 | 仅 Instructor |
| 需要幂等 Key | 是 |
| Path | `courseId`：课程 id；`userId`：被提升用户的 userId（须已是该课成员） |
| 课状态 | 已归档 → `403 COURSE_ARCHIVED` |

**前置条件（目标用户）**

- 已在该课 Enrollment 中，且 `active=true`
- 当前 `courseRole` 必须是 `Student`（已是 TA / Instructor 不能再升）

**Body（可选）**

可不传 body，或传 `{}`：四个权限默认全是 `false`。  
只有显式传 `true` 才会打开对应权限（未传 / `false` / `null` 都当关）。

| 字段 | 类型 | 缺省 | 含义（给前端控能力用） |
|------|------|------|------------------------|
| canGrade | boolean | false | 是否可批改（作业等模块用） |
| canPostAnnouncements | boolean | false | 是否可发公告 |
| canManageGroups | boolean | false | 是否可管小组 |
| canManageCourseEvents | boolean | false | 是否可增删改本课 Events |

```json
{
  "canGrade": true,
  "canPostAnnouncements": false,
  "canManageGroups": false,
  "canManageCourseEvents": true
}
```

**成功 `200`，`data` 形状**

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

- `member`：提升后的成员对象（`courseRole` 变为 `TA`，权限字段已写入）
- `warnings`：字符串数组；当前实现一般为 `[]`，前端可预留展示

**常见错误**

| code | 何时 |
|------|------|
| `NOT_COURSE_INSTRUCTOR` | 调用者不是该课 Instructor |
| `COURSE_ARCHIVED` | 课已归档 |
| `ENROLLMENT_NOT_FOUND` | 目标用户不在该课 |
| `ENROLLMENT_NOT_ACTIVE` | 目标入课记录未激活 |
| `INVALID_ROLE_TRANSITION` | 目标不是 Student（例如已是 TA） |
| `COURSE_NOT_FOUND` | 课程不存在 |

### 8.3 撤销 TA — `DELETE .../members/{userId}/ta`

目标须是 TA。成功后 `courseRole` 变回 `Student`。

### 8.4 改 TA 权限 — `PATCH .../members/{userId}/ta/permissions`

需要幂等 Key；body 为上面四个 boolean。

---

## 9. My courses

### `GET /v2/me/courses`

已登录。返回自己已加入的课（含 Archived），每条带 `role`。

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

用这里的 `role` 决定导航：学生课表 vs 教师备课入口。

---

## 10. Admin enroll

### `POST /v2/admin/courses/{courseId}/enrollments`

| | |
|--|--|
| 谁可以调 | 平台 Admin |
| 需要幂等 Key | 是 |
| Body | `{ "userId": 385 }` |

成功：成员对象，`courseRole: "Student"`。  
非 Admin → `ACCESS_DENIED`；已入课 → `CONFLICT`；已归档 → `COURSE_ARCHIVED`。

---

## 11. Syllabus

前缀：`/v2/courses/{courseId}/syllabus`  
上传仅 PDF；归档后不能上传/恢复。

### 11.1 元信息 — `GET .../syllabus`

已入课。未上传：

```json
{ "posted": false }
```

已上传（教师可能多 `canRestorePrevious`）：

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

### 11.2 预览 / 下载

返回**当前版本**大纲 PDF 的文件流（不是 `{ status, code, data }` JSON）。先调 §11.1，确认 `posted === true` 再请求。通用接法见 [§14](#14-文件流怎么接)。

#### 预览 — `GET /v2/courses/{courseId}/syllabus/preview`

| | |
|--|--|
| 谁可以调 | 已入课成员（Instructor / TA / Student）；Admin 可绕过入课 |
| 需要幂等 Key | 否 |
| 成功 | PDF 二进制流 |
| Content-Type | 多为 `application/pdf` |
| Content-Disposition | `inline; filename="原文件名.pdf"`（浏览器内嵌打开） |

前端建议：带 Bearer 用 `fetch` → `blob` → `URL.createObjectURL`，用 `<iframe>` / 新窗口打开；或同源带 cookie 时直接 `window.open(url)`（本项目以 Bearer 为主，优先 blob）。

#### 下载 — `GET /v2/courses/{courseId}/syllabus/download`

| | |
|--|--|
| 谁可以调 | 同上（已入课 / Admin） |
| 需要幂等 Key | 否 |
| 成功 | PDF 二进制流 |
| Content-Type | 多为 `application/pdf` |
| Content-Disposition | `attachment; filename="原文件名.pdf"`（触发另存为） |

前端建议：`fetch` + Bearer → `blob` → 创建 `<a download={originalFilename}>` 点击；`originalFilename` 可从 §11.1 的 `data.originalFilename` 取。

#### 常见错误

| code | 何时 |
|------|------|
| `NOT_COURSE_MEMBER` | 未入课（非 Admin） |
| `SYLLABUS_NOT_FOUND` | 从未上传大纲，或当前版本文件读失败 |
| `COURSE_NOT_FOUND` | 课程不存在 |
| `UNAUTHORIZED` / `INVALID_TOKEN` | 未登录或 token 无效 |

错误时响应仍可能是 JSON（`Content-Type: application/json`），前端先看响应类型再决定按 JSON 解析还是按文件处理。

### 11.3 上传 — `POST .../syllabus`

上传（或覆盖）当前大纲 PDF。再次上传时：新文件成为 current，旧 current 变为 previous（可供 §11.4 restore）。

| | |
|--|--|
| 方法与路径 | `POST /v2/courses/{courseId}/syllabus` |
| 谁可以调 | 仅 Instructor |
| 需要幂等 Key | 是 |
| Content-Type | `multipart/form-data`（FormData 勿手动设，浏览器自动带 boundary） |
| 课状态 | 已归档 → `403 COURSE_ARCHIVED` |

**表单字段**

| 字段名 | 必填 | 说明 |
|--------|------|------|
| `file` | 是 | **单个** PDF 文件（`@RequestPart("file")`） |

约束：

- 仅 PDF（扩展名 `.pdf`，Content-Type 一般为 `application/pdf`）
- 大小上限默认 **200MB**（配置项 `lms.content.max-file-bytes`）
- 空文件不行

**FormData 示例**

```js
const fd = new FormData();
fd.append("file", pdfFile); // 字段名必须是 file
await fetch(`${BASE}/v2/courses/${courseId}/syllabus`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Idempotency-Key": crypto.randomUUID(),
  },
  body: fd,
});
```

**成功 `200`，`data`**（Instructor 视图，形状同 §11.1 已上传）：

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

第二次及以后上传成功时，`canRestorePrevious` 通常为 `true`（存在上一版可恢复）。

**常见错误**

| code | 何时 |
|------|------|
| `NOT_COURSE_INSTRUCTOR` | 非该课 Instructor |
| `COURSE_ARCHIVED` | 课已归档 |
| `UNSUPPORTED_FILE_TYPE` | 非 PDF |
| `FILE_TOO_LARGE` | 超过大小上限 |
| `BAD_REQUEST` / `PARAM_MISSING` | 未传 `file` 或文件为空 |
| `INTERNAL_SERVER_ERROR` | 存储上传失败 |
| `IDEMPOTENCY_KEY_REQUIRED` | 缺少幂等 Key |

### 11.4 恢复上一版 — `POST .../syllabus/restore`

Instructor；需要幂等 Key。无上一版 → `NO_PREVIOUS_SYLLABUS_VERSION`。

---

## 12. Weeks

前缀：`/v2/courses/{courseId}/weeks`  
新建默认 `Draft`。写：Instructor 且未归档。

### 12.1 列表 — `GET .../weeks`

- **Instructor / Admin**：Draft + Published（全部可见）  
- **TA / Student**：仅 Published（Draft 周不会出现在列表里；TA 与学生相同）

成功单条形状：

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
      "downloadUrl": "http://localhost:8080/api/v2/courses/9/weeks/1/materials/10/download",
      "previewUrl": "http://localhost:8080/api/v2/courses/9/weeks/1/materials/10/preview",
      "createdAt": "2026-07-24T15:01:00",
      "updatedAt": "2026-07-24T15:01:00"
    }
  ]
}
```

`downloadUrl` / `previewUrl`：相对或绝对 API 路径，**请求时仍要带 Bearer**（不要当匿名 CDN）。

### 12.2 创建 — `POST .../weeks`

在课程末尾追加一周，默认未发布。

| | |
|--|--|
| 方法与路径 | `POST /v2/courses/{courseId}/weeks` |
| 谁可以调 | 仅 Instructor（TA / Student → `403 NOT_COURSE_INSTRUCTOR`） |
| 需要幂等 Key | 是 |
| 课状态 | 已归档 → `403 COURSE_ARCHIVED` |

**Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | trim 后非空；最长 **255** 字符 |

```json
{ "title": "Week 1" }
```

**成功 `200`，`data` 示例**

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

- `state` 固定为 `Draft`（学生/TA 列表里还看不到，需再 publish）
- `orderPosition`：接在当前最大序号后（从 0 起）

**常见错误**：`NOT_COURSE_INSTRUCTOR`、`COURSE_ARCHIVED`、`PARAM_MISSING`（缺 title）、`BAD_REQUEST`（title > 255）

---

### 12.3 重命名 — `PATCH .../weeks/{weekId}`

只改标题，不改 `state` / `orderPosition`。

| | |
|--|--|
| 方法与路径 | `PATCH /v2/courses/{courseId}/weeks/{weekId}` |
| 谁可以调 | 仅 Instructor |
| 需要幂等 Key | 是 |
| 课状态 | 已归档 → `COURSE_ARCHIVED` |

**Body**

| 字段 | 必填 | 说明 |
|------|------|------|
| title | 是 | 同创建：非空、≤255 |

```json
{ "title": "Week 1 — Intro" }
```

成功：`200`，`data` 为更新后的 week 对象（含 `materials`）。  
周不存在或不属于该课 → `404 WEEK_NOT_FOUND`。

---

### 12.4 重排 — `PUT .../weeks/reorder`

按传入顺序重写全部周的 `orderPosition`（0, 1, 2, …）。

| | |
|--|--|
| 方法与路径 | `PUT /v2/courses/{courseId}/weeks/reorder` |
| 谁可以调 | 仅 Instructor |
| 需要幂等 Key | 是 |
| 课状态 | 已归档 → `COURSE_ARCHIVED` |

**Body**

| 字段 | 必填 | 说明 |
|------|------|------|
| weekIds | 是 | **当前该课全部 week id 的一个排列**（可含 Draft + Published） |

规则：

- 必须与当前列表的 id **集合完全一致**（不能少、不能多、不能重复、不能塞别的课的 id）
- 数组顺序 = 新的展示顺序：`weekIds[0]` → `orderPosition=0`，以此类推

```json
{ "weekIds": [3, 1, 2] }
```

成功：`200`，`data` 为重排后的 **week 数组**（顺序已更新）。

**常见错误**

| code | 何时 |
|------|------|
| `PARAM_MISSING` | 未传 `weekIds` |
| `BAD_REQUEST` | id 集合与当前不完全一致 |
| `NOT_COURSE_INSTRUCTOR` | 非 Instructor |
| `COURSE_ARCHIVED` | 课已归档 |

前端拖拽排序：先 `GET .../weeks` 拿到全部 id → 按新顺序提交完整 `weekIds`。

---

### 12.5 发布 / 取消发布

- `POST .../weeks/{weekId}/publish`
- `POST .../weeks/{weekId}/unpublish`

均需幂等 Key。

### 12.6 删除 — `DELETE .../weeks/{weekId}`

周内还有资料 → `WEEK_NOT_EMPTY`（先删 materials）。

### 12.7 下载周 ZIP — `GET .../weeks/{weekId}/download.zip`

已入课；学生访问未发布周 → 表现像不存在（`WEEK_NOT_FOUND`）。  
至少 1 个 FILE 资料，否则 `BAD_REQUEST`。见 §14。

---

## 13. Materials

前缀：`/v2/courses/{courseId}/weeks/{weekId}/materials`  

- **上传 / 删除自己的**：Instructor 或 TA（课未归档）
- **重命名 / 重排 / 移动 / 删他人的**：仅 Instructor（课未归档）

### 13.1 创建 — `POST .../materials`

| | |
|--|--|
| 谁可以调 | Instructor 或 TA（课未归档） |
| 需要幂等 Key | 否 |

`multipart/form-data`：

| 字段 | 说明 |
|------|------|
| `files` | 文件，可多个（同名字段多次 / 数组） |
| `linkUrl` | 可选外链 |
| `linkDisplayName` | 可选，外链显示名 |
| | `files` 与 `linkUrl` **至少一种** |

#### 例 1：只上传文件（JS）

```js
const fd = new FormData();
fd.append("files", pdfFile); // 字段名必须是 files；多个文件就多次 append("files", ...)

const res = await fetch(
  "http://localhost:8080/api/v2/courses/9/weeks/1/materials",
  {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` }, // 不要设 Content-Type
    body: fd,
  }
);
const json = await res.json();
// json.data = Material 数组
```

#### 例 2：只加外链（JS）

```js
const fd = new FormData();
fd.append("linkUrl", "https://example.com/reading");
fd.append("linkDisplayName", "Week 1 Reading");

await fetch("http://localhost:8080/api/v2/courses/9/weeks/1/materials", {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` },
  body: fd,
});
```

#### 例 3：curl（文件 + 链接一次提交）

```bash
curl -X POST "http://localhost:8080/api/v2/courses/9/weeks/1/materials" \
  -H "Authorization: Bearer <accessToken>" \
  -F "files=@./slides.pdf" \
  -F "linkUrl=https://example.com/reading" \
  -F "linkDisplayName=Week 1 Reading"
```

#### 成功响应示例（`200`）

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
      "downloadUrl": "http://localhost:8080/api/v2/courses/9/weeks/1/materials/10/download",
      "previewUrl": "http://localhost:8080/api/v2/courses/9/weeks/1/materials/10/preview",
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
      "downloadUrl": "http://localhost:8080/api/v2/courses/9/weeks/1/materials/11/download",
      "previewUrl": null,
      "createdAt": "2026-07-24T15:01:00",
      "updatedAt": "2026-07-24T15:01:00"
    }
  ],
  "message": "Success",
  "timestamp": "2026-07-24T22:00:00Z"
}
```

### 13.2 重命名 — `PATCH .../materials/{materialId}`

仅 Instructor；需要幂等 Key。`{ "displayName": "..." }`。

### 13.3 重排 — `PUT .../materials/reorder`

仅 Instructor；需要幂等 Key。`materialIds` 必须等于该周**全部**资料 id 的排列。

### 13.4 移动 — `POST .../materials/{materialId}/move`

仅 Instructor；需要幂等 Key。`{ "targetWeekId": 12 }`。

### 13.5 删除 — `DELETE .../materials/{materialId}`

| | |
|--|--|
| 谁可以调 | Instructor：任意；TA：仅 `uploadedBy` 为自己的；否则 `403 ACCESS_DENIED` |

### 13.6 预览 / 下载

- `GET .../materials/{materialId}/preview` — 仅可预览类型（看 `previewAvailable`）
- `GET .../materials/{materialId}/download` — FILE 下文件；**LINK 可能 302 跳到外链**

见 §14。不可预览 → `BAD_REQUEST`。

---

## 14. 文件流怎么接

适用于：syllabus preview/download、material preview/download、week `download.zip`。

| 点 | 说明 |
|----|------|
| 鉴权 | 与 JSON 接口相同，带 `Authorization: Bearer` |
| 成功体 | 二进制流，**不是** `{ status, code, data }` |
| 预览 | `Content-Disposition: inline` → `blob` + `URL.createObjectURL` 或新窗口 |
| 下载 | `attachment` → `blob` 后触发 `<a download>`，或用返回的 `downloadUrl` 同源请求 |
| LINK download | 可能 **302 redirect** 到外站；用 `fetch` 时注意 `redirect`；或 `window.open(downloadUrl)`（外链场景） |
| 错误 | 仍可能返回 JSON（如 `SYLLABUS_NOT_FOUND`），前端先看 `Content-Type` |

```js
// 带 token 下载示例
const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
if (!res.ok) {
  const err = await res.json(); // 可能是错误 JSON
  throw err;
}
const blob = await res.blob();
const a = document.createElement("a");
a.href = URL.createObjectURL(blob);
a.download = filenameHint || "file";
a.click();
```

---

## 15. 端点速查表

| 方法 | 路径 | 谁调 | 幂等 Key |
|------|------|------|----------|
| POST | `/v2/courses` | level=INSTRUCTOR | 是 |
| GET | `/v2/courses/{id}` | 已入课 | |
| PUT | `/v2/courses/{id}` | Instructor | 是 |
| DELETE | `/v2/courses/{id}` | Instructor | |
| POST | `/v2/courses/{id}/archive` | Instructor | 是 |
| GET | `/v2/courses/{id}/sessions` | 已入课 | |
| POST/PUT/DELETE | `.../sessions` | Instructor | POST/PUT |
| GET | `/v2/courses/{id}/events` | 已入课 | |
| POST/PUT/DELETE | `.../events` | Instructor / 事件 TA | POST/PUT |
| GET | `/v2/courses/{id}/members` | Instructor | |
| POST/DELETE/PATCH | `.../members/.../ta` | Instructor | POST/PATCH |
| GET | `/v2/me/courses` | 已登录 | |
| POST | `/v2/admin/courses/{id}/enrollments` | Admin | 是 |
| GET/POST | `.../syllabus` | 读：成员；写：Instructor | 写是 |
| GET/POST/... | `.../weeks` | 读：成员；写：Instructor | 多数写是 |
| POST | `.../materials` | Instructor / TA 上传 | 否 |
| DELETE | `.../materials/{id}` | Instructor；TA 仅自己的 | |
| PATCH/PUT/POST move | `.../materials...` | 仅 Instructor | 是 |

---

## 16. 本地测试账号

| 用途 | email | password |
|------|-------|----------|
| 学生 | `regtest1@example.com` … `regtest5@example.com` | `Test12345` |
| 教师 | `teachtest2@example.com` | `Test12345` |

```http
POST /v1/auth/login
Content-Type: application/json

{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "role": "USER"
}
```

响应里取 `data.accessToken` → `Authorization: Bearer ...`。
