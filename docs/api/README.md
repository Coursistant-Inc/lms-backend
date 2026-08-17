# LMS OpenAPI Contracts

Formal API contracts for frontend client generation. Spec format: **OpenAPI 3.0**.

## Files

| Module | Contract |
|--------|----------|
| auth | [`auth.openapi.yaml`](auth.openapi.yaml) |
| user | [`user.openapi.yaml`](user.openapi.yaml) |
| course | [`course.openapi.yaml`](course.openapi.yaml) |
| assignment | [`assignment.openapi.yaml`](assignment.openapi.yaml) |
| quiz | [`quiz.openapi.yaml`](quiz.openapi.yaml) |
| notification | [`notification.openapi.yaml`](notification.openapi.yaml) · [human notes](notification.md) |

Base URL / server: `/api` (matches `server.servlet.context-path`).

## Auth

- Default security: HTTP Bearer JWT (`Authorization: Bearer <accessToken>`).
- Public endpoints (no bearer) match `AuthPublicPaths` exactly, including `GET /v1` and auth session POSTs under `/v1/auth/*`.
- Login / register / refresh set HttpOnly `refreshToken` cookie (`Path=/api`, `SameSite=Lax`, `Secure` when request is secure).
- Refresh / logout read the `refreshToken` cookie.

## Generate (export)

Does **not** require a manually started server or local MySQL. Uses the `openapi` Spring profile (H2 + mocked Redis/MinIO/Mail) via Failsafe IT:

```powershell
./scripts/export-openapi.ps1 -Module all
# or a single module:
./scripts/export-openapi.ps1 -Module auth
```

Writes `docs/api/{module}.openapi.yaml`.

## Verify (parser + inventory + security + naming + drift)

```powershell
./scripts/verify-openapi.ps1 -Module all
```

Re-exports to `target/openapi-check/` and fails if committed YAML drifts or inventory/security/naming checks fail.

## Live Swagger UI

With the app running: `/api/swagger-ui/index.html` — select groups `auth`, `user`, `course`, `assignment`, `quiz`, `notification`.

YAML endpoints: `/api/v3/api-docs.yaml/{group}`.

## Versioning

- Spec `info.version` is set in `OpenApiConfig` (`1.0.0`).
- Breaking changes after initial publish require explicit review; regeneration alone must not silently land incompatible clients.
- Stable identifiers: every operation has a unique `operationId`; public schemas must not use springdoc auto suffixes (`_1`, `_2`, …).

## Frontend consumption

Point your generator (openapi-generator, orval, @hey-api/openapi-ts, etc.) at the module YAML you need, or at the live `/api/v3/api-docs.yaml/{group}` URL against a running environment.
