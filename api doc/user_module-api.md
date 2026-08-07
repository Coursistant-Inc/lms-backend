# User 模块 API 参考（前端）

给前端联调 / 写页面用（个人资料、头像、账号管理）。  
**登录 / 注册 / 刷新 / 登出 / 验证码 / 改密 / Managed users** 见独立文档：[`auth_module-api.md`](./auth_module-api.md)。

Base URL：`http://localhost:8080/api`

远程环境可用：`https://dev.xlearnedu.com:8080/api`

---

## 目录

1. [怎么调用](#1-怎么调用)
2. [典型页面流程](#2-典型页面流程)
3. [角色与权限（产品约定）](#3-角色与权限产品约定)
4. [枚举速查](#4-枚举速查)
5. [Auth（详见 Auth 文档）](#5-auth详见-auth-文档)
6. [邮箱验证与密码（详见 Auth 文档）](#6-邮箱验证与密码详见-auth-文档)
7. [Profile（我的资料）](#7-profile我的资料)
8. [Avatar（头像）](#8-avatar头像)
9. [Users 账号管理](#9-users-账号管理)
10. [Admins 管理](#10-admins-管理)
11. [头像二进制怎么接](#11-头像二进制怎么接)
12. [端点速查表](#12-端点速查表)
13. [本地测试账号](#13-本地测试账号)

---

## 1. 怎么调用

### 1.1 Header

| Header | 何时需要 |
|--------|----------|
| `Authorization: Bearer {accessToken}` | 几乎所有接口（先 `POST /v1/auth/login` 或 register） |
| `Idempotency-Key: {uuid}` | **仅下列写接口**（每次请求用新 UUID） |
| `Content-Type: application/json` | JSON body |
| `Content-Type: multipart/form-data` | 头像上传（浏览器 FormData 会自动带） |

**需要 `Idempotency-Key` 的写接口（本模块相关）：**

- `PUT /v1/auth/password`、`POST /v1/auth/password-resets`（详见 Auth 文档）
- `PATCH /v2/me/profile`
- `DELETE /v2/me/profile/avatar`
- `POST/PUT/DELETE/PATCH /v2/users...`（除 GET）
- Managed users 写接口（见 Auth 文档）

缺 Key → `IDEMPOTENCY_KEY_REQUIRED`。

**不需要** Key：`login` / `register` / `refresh-token` / `logout`、发验证码、`PUT .../avatar`、所有 GET。

> **已删除** `.../email-verifications/*/validate` 端点；验证码在 register / password-resets 内消费。

Token 无效 → `401`（`INVALID_TOKEN` / `UNAUTHORIZED`）。

### 1.2 可匿名访问（JWT 不校验）

- `GET /v1`
- `POST /v1/auth/login`、`register`、`refresh-token`、`logout`
- `POST /v1/auth/email-verifications/register`、`.../reset`
- `POST /v1/auth/password-resets`
- `GET /v2/users/{userId}/avatar`

注意：`PUT /v1/auth/password` **需要** Bearer；`logout` **不需要** Bearer（靠 Cookie）。

### 1.3 Cookie：`refreshToken`

| | |
|--|--|
| 谁设置 | `login` / `register` / `refresh-token` 成功时 `Set-Cookie` |
| 属性 | HttpOnly、Secure、SameSite=Lax、path=/、约 14 天 |
| JSON 里有没有 | **没有**。`AuthResult.refreshToken` 只用于服务端写 Cookie，不进响应 body |
| 前端怎么用 | 同源请求默认带 Cookie；`refresh-token` 靠 Cookie，不要自己拼 refresh 到 Header |

### 1.4 统一 JSON 响应

成功看 `data`；失败看 `code`（不要只看 HTTP status）。

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

头像二进制流 **不是**这层 JSON，见 [§11](#11-头像二进制怎么接)。

### 1.5 字段名注意

| 接口 | 头像字段 |
|------|----------|
| `POST /v1/auth/login`（USER）/ `register` | `avatar`（已是可访问的代理 URL） |
| `GET/PATCH /v2/me/profile`、上传/删除头像 | `avatarUrl` |
| `GET /v2/users/{id}` 管理接口 | `avatar`（库内 **MinIO object key**，不是 URL） |

前端展示请用 `avatar` / `avatarUrl` 响应值，或拼 `GET /v2/users/{userId}/avatar`；**不要**把管理接口返回的 key 当图片地址。

### 1.6 密码规则

至少 **8** 位，且同时含**字母**和**数字**。否则 `INVALID_PASSWORD_FORMAT`。

---

## 2. 典型页面流程

### 2.1 登录进站 / 注册 / 刷新 / 登出 / 改密

完整流程图与请求字段见 [`auth_module-api.md` §2](./auth_module-api.md#2-典型业务流程图)。摘要：

| 流程 | API 顺序 |
|------|----------|
| 登录 | `POST /v1/auth/login` → 存 `accessToken` →（可选）`GET /v2/me/profile` |
| 注册 | 发码 → `POST /v1/auth/register`（含 `verificationCode`；**无** validate） |
| 刷新 | Cookie → `POST /v1/auth/refresh-token` |
| 登出 | `POST /v1/auth/logout`（匿名可调，靠 Cookie） |
| 忘记密码 | 发 reset 码 → `POST /v1/auth/password-resets`（含 `verificationCode`） |
| 已登录改密 | `PUT /v1/auth/password`（`currentPassword` / `newPassword`） |

### 2.2 个人资料 / 头像

1. `GET /v2/me/profile`
2. 改显示名 / 邮件通知：`PATCH /v2/me/profile`（需幂等 Key）
3. 换头像：`PUT /v2/me/profile/avatar`（`multipart` 字段名 `file`）
4. 删头像：`DELETE /v2/me/profile/avatar`（需幂等 Key）
5. `<img src="{avatarUrl}">` 即可；他人头像用返回的 URL 或 `GET /v2/users/{id}/avatar`

---

## 3. 角色与权限（产品约定）

完整登录路由与能力矩阵见 [`auth_module-api.md` §3](./auth_module-api.md#3-角色与登录路由)。摘要：

| 字段 | 取值 | 含义 |
|------|------|------|
| 登录 body `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` \| `ADMIN`（兼容） | 决定查 user / admin 表 |
| JWT `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` | 鉴权角色 |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` 等 | 仅 user 有意义；注册默认 `STUDENT` |

| 能力 | USER | TENANT_ADMIN | SYSTEM_ADMIN |
|------|:----:|:------------:|:------------:|
| 自助注册 / profile / 头像 | ✓ | profile ✓ | 无 `/me/profile` |
| `/v2/users` CRUD | 产品上后台用 | | 推荐 |
| `/v2/admins` 读 | | | ✓（写接口已禁用） |
| Managed users | | 租户侧 | 系统侧 |

> 课程里的 Instructor / TA / Student 见 Course 文档；与这里的平台 `level` 不是同一套字段。

常见错误：`INVALID_CREDENTIALS`、`USER_NOT_FOUND`、`UNAUTHORIZED`、`INVALID_TOKEN`。

---

## 4. 枚举速查

| 字段 | 合法值 |
|------|--------|
| 登录 body `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` \| `ADMIN` |
| JWT / Profile `role` | `USER` \| `TENANT_ADMIN` \| `SYSTEM_ADMIN` |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `NOT_APPLICABLE` 等 |
| 列表教师 query | `role=instructor` 或 `role=teacher`（`GET /v2/users`） |

非法 / 缺必填 → 多为 `PARAM_MISSING` 或 `400 BAD_REQUEST`。

### 4.1 `tenantId`

`user.tenant_id` **NOT NULL**；存量已回填为 `1`。

| 场景 | 规则 |
|------|------|
| 公开注册 `POST /v1/auth/register` | 服务端**始终绑定租户 1**（忽略客户端 `tenantId`） |
| Admin `POST /v2/users` | body **必填** `tenantId`，可为任意已存在租户；缺 → `PARAM_MISSING`；不存在 → `TENANT_NOT_FOUND` |
| 改用户租户 | **仅** `PATCH /v2/admin/users/{id}/tenant`（SYSTEM_ADMIN + 幂等 Key）。有 enrollment / 授课 / 创建课程 → `409 USER_TENANT_CHANGE_BLOCKED` |
| `PUT /v2/users/{id}` | body **禁止**带 `tenantId`（带了 → `400 BAD_REQUEST`） |
| Profile / 密码 / 头像 | **不**读写租户 |

详见 [`tenant_module-api.md`](./tenant_module-api.md)。

---

## 5. Auth（详见 Auth 文档）

Session、登录/注册/刷新/登出的请求/响应/错误码见：

→ [`auth_module-api.md` §4 Session API](./auth_module-api.md#4-session-api)

本文档不再维护重复的 Auth 端点明细。

---

## 6. 邮箱验证与密码（详见 Auth 文档）

发码、改密、重置（含 `verificationCode`、无 validate 端点）见：

→ [`auth_module-api.md` §5](./auth_module-api.md#5-邮箱验证与密码)

---

## 7. Profile（我的资料）

前缀：`/v2/me/profile`  
身份：JWT → 当前 `userId`；无则 `UNAUTHORIZED`。

### 7.1 获取 — `GET /v2/me/profile`

| | |
|--|--|
| 鉴权 | 需要 Bearer |
| 需要幂等 Key | 否 |

成功 `data`（`ProfileResponse`）：

```json
{
  "userId": 385,
  "displayName": "Alex Rivera",
  "email": "regtest1@example.com",
  "role": "USER",
  "level": "STUDENT",
  "avatarUrl": "http://localhost:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f",
  "emailNotifications": true
}
```

- 无头像：`avatarUrl` 为 `null`
- DB 中 `emailNotifications` 为 null 时按 **true** 返回

---

### 7.2 更新 — `PATCH /v2/me/profile`

| | |
|--|--|
| 鉴权 | 需要 Bearer |
| 需要幂等 Key | **是** |
| Body | 至少一项 |

| 字段 | 类型 | 说明 |
|------|------|------|
| displayName | string | trim 后 1–100 字符；对应库字段 `name` |
| emailNotifications | boolean | 是否接收邮件通知 |

```json
{
  "displayName": "Alex Rivera",
  "emailNotifications": false
}
```

成功：`data` 为更新后的 `ProfileResponse`。

错误：`PARAM_MISSING`（一个字段都没传）、`BAD_REQUEST`（displayName 非法）、`USER_NOT_FOUND`、`IDEMPOTENCY_*`。

---

## 8. Avatar（头像）

### 8.1 上传自己的头像 — `PUT /v2/me/profile/avatar`

| | |
|--|--|
| 鉴权 | 需要 Bearer |
| 需要幂等 Key | **否** |
| Content-Type | `multipart/form-data` |
| 字段名 | **`file`**（`@RequestPart("file")`） |

限制：

- 最大 **5MB**
- 类型：JPG / JPEG / PNG（看 Content-Type 或扩展名）

成功：`data` 为 `ProfileResponse`（含新 `avatarUrl`，带新的 `?v=` 缓存戳）。

错误：`INVALID_AVATAR_FILE`、`USER_NOT_FOUND`。

```js
const form = new FormData();
form.append("file", fileInput.files[0]); // 字段名必须是 file

await fetch(`${base}/v2/me/profile/avatar`, {
  method: "PUT",
  headers: { Authorization: `Bearer ${token}` },
  // 不要手动设 Content-Type，让浏览器带 boundary
  body: form,
});
```

---

### 8.2 删除自己的头像 — `DELETE /v2/me/profile/avatar`

| | |
|--|--|
| 鉴权 | 需要 Bearer |
| 需要幂等 Key | **是** |

无头像再删也 `200`（幂等）。成功：`avatarUrl` 为 `null`。

---

### 8.3 读取头像流 — `GET /v2/users/{userId}/avatar`

| | |
|--|--|
| 鉴权 | **公开**（可不带 Bearer） |
| 成功体 | 图片二进制，**不是** `ApiResponse` |
| 缓存 | `Cache-Control: private, max-age=300` |
| Content-Type | `image/jpeg` 或 `image/png` |

可选 query：`?v={uuid}`（登录 / profile 返回的 URL 已带；用于缓存失效）。

无用户 / 无头像 / 拉取失败 → `NOT_FOUND`。

**URL 形态**（服务端 `AvatarUrlBuilder`）：

```text
{contextPath}/v2/users/{userId}/avatar?v={objectKey文件名中的uuid}
```

例：`http://localhost:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f`

DB 存的是 MinIO key（如 `385/xxxx.png`），前端只用响应里的完整 URL。

---

## 9. Users 账号管理

前缀：`/v2/users`  
均需 Bearer。写操作需幂等 Key。  
**产品约定：仅 Admin 后台使用**；代码层目前未强制 Admin。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v2/users` | 创建；至少 `email`+`password`+**`tenantId`**；`role` 强制 `USER` |
| GET | `/v2/users/{id}` | 详情；`USER_NOT_FOUND` |
| GET | `/v2/users` | 列表；可按 User 字段 query 过滤；`?role=instructor` / `teacher` → 仅教师 |
| PUT | `/v2/users/{id}` | 更新；**不可**改 `tenantId`（body 带了 → `400`） |
| DELETE | `/v2/users/{id}` | 删除 |
| DELETE | `/v2/users/batch` | body：`[1,2,3]` |
| PATCH | `/v2/users/{id}/password-status` | 将 `mustChangePassword=false` |
| PATCH | `/v2/admin/users/{id}/tenant` | **Admin 专用**；改租户（见下） |

`User` 常见字段：`id` **`tenantId`** `username` `password` `name` `avatar`（**key**）`role` `level` `email` `mustChangePassword` `emailNotifications`。

**创建 Body 要点**

| 字段 | 必填 | 说明 |
|------|------|------|
| email / password | 是 | |
| tenantId | **是** | 任意已存在租户；缺 → `PARAM_MISSING`；不存在 → `TENANT_NOT_FOUND`。当前前端写死传 `1` |
| name / username / level | 否 | `level` 默认 `STUDENT` |

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

### 9.1 更改用户租户 — `PATCH /v2/admin/users/{id}/tenant`

| | |
|--|--|
| 鉴权 | JWT `role=ADMIN`；否则 `403 ACCESS_DENIED` |
| 需要幂等 Key | **是** |

**Body**

```json
{ "tenantId": 1 }
```

- `tenantId` 必填；租户须存在
- 目标用户已有 enrollment，或作为 instructor/creator 关联课程 → `409 USER_TENANT_CHANGE_BLOCKED`
- 与现值相同 → 成功（幂等）

> 管理接口返回的 `avatar` 是存储 key，不是可直接 `<img>` 的 URL。展示请用 §8.3。  
> 勿在 UI 展示 `password`。

错误示例：`USER_ALREADY_EXISTS`、`PARAM_MISSING`、`INVALID_PASSWORD_FORMAT`、`USER_NOT_FOUND`、`TENANT_NOT_FOUND`、`USER_TENANT_CHANGE_BLOCKED`、`ACCESS_DENIED`。

---

## 10. Admins 管理

前缀：`/v2/admins`。鉴权：`SYSTEM_ADMIN`。  
**读接口可用；写接口 Phase 2 前一律 `403 Forbidden`。**  
详情与 Managed users 见 [`auth_module-api.md` §6–§7](./auth_module-api.md#6-admins只读)。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v2/admins/{id}` | 查单个 |
| GET | `/v2/admins` | 列表；query 过滤 |
| POST / PUT / DELETE | `/v2/admins...` | **禁用** |

Admin **没有** `/v2/me/profile`。登录用 `role: "ADMIN"` 或 `"SYSTEM_ADMIN"`。

---

## 11. 头像二进制怎么接

适用于：`GET /v2/users/{userId}/avatar`。

| 点 | 说明 |
|----|------|
| 鉴权 | 公开；带不带 Bearer 均可 |
| 成功体 | 图片二进制，**不是** `{ status, code, data }` |
| 推荐 | 直接把 `avatarUrl` / `avatar` 填进 `<img src>` |
| 错误 | 可能仍是 JSON（如 `NOT_FOUND`），先看 `Content-Type` |
| 缓存 | 换头像后 URL 的 `?v=` 会变，旧链接可缓存约 5 分钟 |

```js
// 资料页：优先用接口返回的 avatarUrl
<img src={profile.avatarUrl} alt="" />

// 上传后用响应里的新 avatarUrl 替换本地 state，无需手动拼
```

---

## 12. 端点速查表

Auth 全量速查见 [`auth_module-api.md` §8](./auth_module-api.md#8-端点速查表)。本模块常用：

| 方法 | 路径 | 谁调 | 幂等 Key |
|------|------|------|----------|
| GET | `/v2/me/profile` | 当前 USER / TENANT_ADMIN | |
| PATCH | `/v2/me/profile` | 当前 USER / TENANT_ADMIN | 是 |
| PUT | `/v2/me/profile/avatar` | 当前 USER / TENANT_ADMIN | 否 |
| DELETE | `/v2/me/profile/avatar` | 当前 USER / TENANT_ADMIN | 是 |
| GET | `/v2/users/{userId}/avatar` | 公开 | |
| GET | `/v2/users`、`/v2/users/{id}` | 已登录（产品：后台） | |
| POST/PUT/DELETE/PATCH | `/v2/users...` | 已登录（产品：后台） | 是 |
| PATCH | `/v2/admin/users/{id}/tenant` | SYSTEM_ADMIN | 是 |
| GET | `/v2/admins`、`/v2/admins/{id}` | SYSTEM_ADMIN | |

---

## 13. 本地测试账号

| 用途 | email | password | 说明 |
|------|-------|----------|------|
| 学生 | `regtest1@example.com` … `regtest5@example.com` | `Test12345` | `role=USER`，`level=STUDENT` |
| 教师 | `teachtest2@example.com` | `Test12345` | `role=USER`，`level=INSTRUCTOR` |
| 平台 Admin | `admin@example.com` | `Test12345` | 登录 `role=ADMIN` 或 `SYSTEM_ADMIN` |

更多 Auth 说明见 [`auth_module-api.md` §9](./auth_module-api.md#9-本地测试账号)。

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
资料联调：`GET /v2/me/profile`；头像联调：`PUT /v2/me/profile/avatar`（字段名 `file`）。
