# User 模块 API 参考（前端）

给前端联调 / 写页面用（登录注册、个人资料、头像、账号管理）。  
Base URL：`http://localhost:8080/api`

远程环境可用：`https://dev.xlearnedu.com:8080/api`

---

## 目录

1. [怎么调用](#1-怎么调用)
2. [典型页面流程](#2-典型页面流程)
3. [角色与权限（产品约定）](#3-角色与权限产品约定)
4. [枚举速查](#4-枚举速查)
5. [Auth：登录 / 注册 / 登出 / 刷新](#5-auth登录--注册--登出--刷新)
6. [邮箱验证与密码](#6-邮箱验证与密码)
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

**需要 `Idempotency-Key` 的写接口：**

- `POST /v1/auth/email-verifications/register/validate`
- `POST /v1/auth/email-verifications/reset/validate`
- `PUT /v1/auth/password`
- `POST /v1/auth/password-resets`
- `PATCH /v2/me/profile`
- `DELETE /v2/me/profile/avatar`
- `POST/PUT/DELETE/PATCH /v2/users...`（除 GET）
- `POST/PUT/DELETE /v2/admins...`（除 GET）

缺 Key → `IDEMPOTENCY_KEY_REQUIRED`。

**不需要** Key：`login` / `register` / `refresh-token` / `logout`、发验证码、`PUT .../avatar`、所有 GET。

Token 无效 → `401`（`INVALID_TOKEN` / `UNAUTHORIZED`）。

### 1.2 可匿名访问（JWT 不校验）

- `GET /v1`
- `POST /v1/auth/login`、`register`、`refresh-token`
- `POST /v1/auth/email-verifications/**`
- `POST /v1/auth/password-resets`
- `GET /v2/users/{userId}/avatar`

注意：`PUT /v1/auth/password`、`POST /v1/auth/logout` **需要** Bearer。

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

### 2.1 登录进站

1. `POST /v1/auth/login`（`role: "USER"`）→ 存 `data.accessToken`
2. Cookie 自动带 `refreshToken`（勿读 JSON）
3. 进资料页：`GET /v2/me/profile`
4. Token 将过期：`POST /v1/auth/refresh-token`（靠 Cookie）→ `data` 为新 accessToken 字符串
5. 退出：`POST /v1/auth/logout`（需 Bearer）

### 2.2 注册

1. `POST /v1/auth/email-verifications/register?email=...` 发验证码
2. `POST /v1/auth/email-verifications/register/validate?email=...&code=...`（需幂等 Key）
3. `POST /v1/auth/register`（`email` `password` `name` **`tenantId`**；可选 `username`）。当前前端写死传 `tenantId: 1`
4. 成功同登录：拿 `accessToken` + refresh Cookie；固定 `role=USER`、`level=STUDENT`

### 2.3 个人资料 / 头像

1. `GET /v2/me/profile`
2. 改显示名 / 邮件通知：`PATCH /v2/me/profile`（需幂等 Key）
3. 换头像：`PUT /v2/me/profile/avatar`（`multipart` 字段名 `file`）
4. 删头像：`DELETE /v2/me/profile/avatar`（需幂等 Key）
5. `<img src="{avatarUrl}">` 即可；他人头像用返回的 URL 或 `GET /v2/users/{id}/avatar`

### 2.4 忘记密码

1. `POST /v1/auth/email-verifications/reset?email=...`
2. `POST /v1/auth/email-verifications/reset/validate?email=...&code=...`（需幂等 Key）
3. `POST /v1/auth/password-resets`，body：`{ "email", "newPassword" }`（需幂等 Key）

### 2.5 已登录改密

1. `PUT /v1/auth/password`（需 Bearer + 幂等 Key）
2. body：`email`、`password`（旧）、`newPassword`、`role`（`USER` 或 `ADMIN`）
3. 成功后该用户 refresh token 会被清掉，需重新登录拿 Cookie

---

## 3. 角色与权限（产品约定）

平台身份两层：

| 字段 | 取值 | 含义 |
|------|------|------|
| `role` | `USER` \| `ADMIN` | 登录 body / JWT 里的平台角色；**必须与账号类型一致** |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `SELF` | 仅 USER 有意义；注册默认 `STUDENT` |

| 能力 | 学生 / 教师（USER） | 平台 Admin |
|------|:------------------:|:----------:|
| 登录 / 刷新 / 登出 | ✓（`role=USER`） | ✓（`role=ADMIN`） |
| 自助注册 | ✓ | |
| 看 / 改自己的 profile、头像 | ✓ | （Admin 走 `/v2/admins`，无 `/me/profile`） |
| 改自己的密码 | ✓ | ✓ |
| `/v2/users` CRUD、列表教师 | 代码层**未做 Admin 门禁**；**产品上仅后台 Admin 使用** | ✓（推荐） |
| `/v2/admins` CRUD | 同上，前端勿对学生开放 | ✓ |

> 课程里的 Instructor / TA / Student 见 Course 文档；与这里的平台 `level` 不是同一套字段。

常见错误：`INVALID_CREDENTIALS`、`ACCOUNT_LOCKED`、`USER_NOT_FOUND`、`UNAUTHORIZED`、`INVALID_TOKEN`。

---

## 4. 枚举速查

| 字段 | 合法值 |
|------|--------|
| `role`（登录 / AuthResult / Profile） | `USER` \| `ADMIN` |
| `level` | `STUDENT` \| `TA` \| `INSTRUCTOR` \| `SELF` |
| 列表教师 query | `role=instructor` 或 `role=teacher`（`GET /v2/users`） |

非法 / 缺必填 → 多为 `PARAM_MISSING` 或 `400 BAD_REQUEST`。

### 4.1 `tenantId`（必传）

`user.tenant_id` **NOT NULL**；存量已回填为 `1`。后端**不**静默默认。

| 场景 | 规则 |
|------|------|
| 公开注册 `POST /v1/auth/register`、OAuth 注册 | body **必填** `tenantId`；**仅允许** `1`，否则 `400 BAD_REQUEST`。当前前端写死传 `1` |
| Admin `POST /v2/users` | body **必填** `tenantId`，可为任意已存在租户；缺 → `PARAM_MISSING`；不存在 → `TENANT_NOT_FOUND` |
| 改用户租户 | **仅** `PATCH /v2/admin/users/{id}/tenant`（Admin + 幂等 Key）。有 enrollment / 授课 / 创建课程 → `409 USER_TENANT_CHANGE_BLOCKED` |
| `PUT /v2/users/{id}` | body **禁止**带 `tenantId`（带了 → `400 BAD_REQUEST`） |
| Profile / 密码 / 头像 | **不**读写租户 |

详见 [`tenant_module-api.md`](./tenant_module-api.md)。

---

## 5. Auth：登录 / 注册 / 登出 / 刷新

前缀：`/v1/auth`（健康检查：`GET /v1`）

### 5.1 健康检查 — `GET /v1`

无需登录。成功：`data` 为 `"访问成功"`。

---

### 5.2 登录 — `POST /v1/auth/login`

| | |
|--|--|
| 鉴权 | 否 |
| 需要幂等 Key | 否 |

**Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | |
| password | string | 是 | |
| role | string | 是 | `USER` 或 `ADMIN`，必须与账号类型一致 |

```json
{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "role": "USER"
}
```

成功 `data`（`AuthResult`）：

```json
{
  "userId": 385,
  "email": "regtest1@example.com",
  "name": "Alex Rivera",
  "username": "regtest1",
  "role": "USER",
  "level": "STUDENT",
  "avatar": "http://localhost:8080/api/v2/users/385/avatar?v=15173feacb804aa39573c818df203e3f",
  "accessToken": "eyJ..."
}
```

- 同时 `Set-Cookie: refreshToken=...`
- 无头像时 `avatar` 可为 `null`
- ADMIN 登录时 `avatar` 可能是库内原始值，不一定是代理 URL

错误：`PARAM_MISSING`、`USER_NOT_FOUND`、`INVALID_CREDENTIALS`、`ACCOUNT_LOCKED`（连续失败约 ≥6 次会锁一段时间）、`TOKEN_CREATION_FAILED`。

---

### 5.3 注册 — `POST /v1/auth/register`

| | |
|--|--|
| 鉴权 | 否 |
| 需要幂等 Key | 否 |
| 前置 | 须先完成 register 邮箱验证（约 15 分钟有效） |

**Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | |
| password | string | 是 | 见 §1.6 |
| name | string | 是 | 显示名 |
| tenantId | int | **是** | 公开注册**仅允许 `1`**（种子 Default）。缺 → `PARAM_MISSING`；非 1 → `BAD_REQUEST`。当前前端写死传 `1` |
| username | string | 否 | 默认取邮箱 `@` 前缀 |

```json
{
  "email": "newuser@example.com",
  "password": "Test12345",
  "name": "New User",
  "tenantId": 1
}
```

成功：形状同登录 `AuthResult` + refresh Cookie。  
固定：`role=USER`、`level=STUDENT`、`emailNotifications=true`。

错误：`PARAM_MISSING`、`INVALID_VERIFICATION_CODE`（未验证）、`INVALID_PASSWORD_FORMAT`、`BAD_REQUEST`（邮箱已存在、`tenantId≠1` 等，文案可能防枚举）、`TENANT_NOT_FOUND`。

---

### 5.4 刷新 Token — `POST /v1/auth/refresh-token`

| | |
|--|--|
| 鉴权 | 否（靠 Cookie `refreshToken`） |
| 需要幂等 Key | 否 |
| 限流 | 同 IP 约 60s 内 >10 次 → `TOO_MANY_REQUESTS` |

成功：`data` 为**新 accessToken 字符串**（不是对象）；并轮换 refresh Cookie。

```json
{
  "status": 200,
  "code": "SUCCESS",
  "data": "eyJ...",
  "message": "Success"
}
```

错误：`REFRESH_TOKEN_INVALID`、`TOO_MANY_REQUESTS`。

---

### 5.5 登出 — `POST /v1/auth/logout`

| | |
|--|--|
| 鉴权 | **需要** Bearer |
| 需要幂等 Key | 否 |

删除服务端 refresh，并清空 Cookie。成功：`data` 为 `null`。

---

## 6. 邮箱验证与密码

### 6.1 发注册验证码 — `POST /v1/auth/email-verifications/register`

| | |
|--|--|
| 鉴权 | 否 |
| 参数 | `email`（query / form；当前实现未标 `@RequestParam`，建议 query：`?email=`） |

已注册邮箱也会返回成功（静默，防枚举）。

限流相关错误：`VERIFICATION_RESEND_COOLDOWN`（约 60s）、`VERIFICATION_HOURLY_LIMIT`（约 5 次/小时）。

---

### 6.2 校验注册验证码 — `POST /v1/auth/email-verifications/register/validate`

| | |
|--|--|
| 鉴权 | 否 |
| 需要幂等 Key | **是** |
| 参数 | query：`email`、`code` |

成功后写入「已验证」标记（约 15 分钟），再调注册。

错误：`INVALID_VERIFICATION_CODE`、`VERIFICATION_CODE_EXPIRED`（码约 10 分钟）、`VERIFICATION_ATTEMPTS_EXCEEDED`（错约 5 次）、`PARAM_MISSING`、`IDEMPOTENCY_*`。

---

### 6.3 发重置验证码 — `POST /v1/auth/email-verifications/reset`

同 §6.1 形态；用户不存在也静默成功。

---

### 6.4 校验重置验证码 — `POST /v1/auth/email-verifications/reset/validate`

同 §6.2；需幂等 Key。通过后再调 §6.6。

---

### 6.5 已登录改密 — `PUT /v1/auth/password`

| | |
|--|--|
| 鉴权 | **需要** Bearer |
| 需要幂等 Key | **是** |

**Body**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | |
| password | string | 是 | 旧密码 |
| newPassword | string | 是 | 新密码，见 §1.6 |
| role | string | 建议带 | `USER` 或 `ADMIN` |

```json
{
  "email": "regtest1@example.com",
  "password": "Test12345",
  "newPassword": "Test12345a",
  "role": "USER"
}
```

错误：`PARAM_MISSING`、`USER_NOT_FOUND`、`INVALID_PASSWORD`、`INVALID_PASSWORD_FORMAT`、`IDEMPOTENCY_*`。

---

### 6.6 忘记密码重置 — `POST /v1/auth/password-resets`

| | |
|--|--|
| 鉴权 | 否 |
| 需要幂等 Key | **是** |
| 前置 | 须先完成 reset 邮箱验证 |

**Body**

```json
{
  "email": "regtest1@example.com",
  "newPassword": "Test12345a"
}
```

错误：`PARAM_MISSING`、`INVALID_VERIFICATION_CODE`、`INVALID_PASSWORD_FORMAT`、`BAD_REQUEST`。

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

前缀：`/v2/admins`  
对称 CRUD；需 Bearer；写操作需幂等 Key。  
**产品约定：仅平台 Admin 后台**。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v2/admins` | 创建；邮箱重复 → `USER_ALREADY_EXISTS`；无密码时可能用服务端默认密码 |
| GET | `/v2/admins/{id}` | |
| GET | `/v2/admins` | query 过滤 |
| PUT | `/v2/admins/{id}` | |
| DELETE | `/v2/admins/{id}` | |
| DELETE | `/v2/admins/batch` | body：id 列表 |

常见字段：`id` `username` `password` `name` `avatar` `role` `phone` `email` `invitation` 等。

Admin **没有** `/v2/me/profile`；资料改走本表 CRUD。登录用 `role: "ADMIN"`。

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

| 方法 | 路径 | 谁调 | 幂等 Key |
|------|------|------|----------|
| GET | `/v1` | 匿名 | |
| POST | `/v1/auth/login` | 匿名 | |
| POST | `/v1/auth/register` | 匿名（需先邮箱验证） | |
| POST | `/v1/auth/refresh-token` | Cookie | |
| POST | `/v1/auth/logout` | 已登录 | |
| POST | `/v1/auth/email-verifications/register` | 匿名 | |
| POST | `/v1/auth/email-verifications/register/validate` | 匿名 | 是 |
| POST | `/v1/auth/email-verifications/reset` | 匿名 | |
| POST | `/v1/auth/email-verifications/reset/validate` | 匿名 | 是 |
| PUT | `/v1/auth/password` | 已登录 | 是 |
| POST | `/v1/auth/password-resets` | 匿名（需先验证） | 是 |
| GET | `/v2/me/profile` | 当前 USER | |
| PATCH | `/v2/me/profile` | 当前 USER | 是 |
| PUT | `/v2/me/profile/avatar` | 当前 USER | 否 |
| DELETE | `/v2/me/profile/avatar` | 当前 USER | 是 |
| GET | `/v2/users/{userId}/avatar` | 公开 | |
| GET | `/v2/users`、`/v2/users/{id}` | 已登录（产品：Admin） | |
| POST/PUT/DELETE/PATCH | `/v2/users...` | 已登录（产品：Admin） | 是 |
| PATCH | `/v2/admin/users/{id}/tenant` | Admin | 是 |
| GET/POST/PUT/DELETE | `/v2/admins...` | 已登录（产品：Admin） | 写是 |

---

## 13. 本地测试账号

| 用途 | email | password | 说明 |
|------|-------|----------|------|
| 学生 | `regtest1@example.com` … `regtest5@example.com` | `Test12345` | `role=USER`，`level=STUDENT` |
| 教师 | `teachtest2@example.com` | `Test12345` | `role=USER`，`level=INSTRUCTOR` |

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
