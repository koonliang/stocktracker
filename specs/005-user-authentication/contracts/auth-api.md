# Contract: Dev-mode Auth REST API

Base path `/api/auth` (active when `stocktracker.auth.mode=dev`). All requests/
responses are JSON. Responses are **non-enumerating**: sign-up and forgot-password
return the same success shape whether or not the email exists. JAX-RS resource:
`AuthResource`. Errors use the existing `ApiErrorResponse {code,message,details}`.

## POST /api/auth/signup
Create an unverified account and issue an email-verification token (FR-009/011).

Request: `{ "email": string, "password": string }`
Responses:
- `202 Accepted` `{ "status": "verification_sent" }` — always, for valid input,
  regardless of whether the email already existed (FR-014, non-enumerating).
- `400 Bad Request` `code=VALIDATION` — invalid email or password policy (FR-010),
  with `details` listing unmet rules.
- `429 Too Many Requests` — rate limited (FR-020).

## POST /api/auth/verify-email
Activate an account using the verification token (FR-013).

Request: `{ "token": string }`
Responses:
- `200 OK` `{ "status": "verified" }` — account set ACTIVE.
- `400 Bad Request` `code=TOKEN_INVALID` — unknown/expired/already-consumed.

## POST /api/auth/resend-verification
Re-issue verification for an unverified account (FR-012). Supersedes prior token.

Request: `{ "email": string }`
Response: `202 Accepted` `{ "status": "verification_sent" }` (non-enumerating).

## POST /api/auth/login
Verify credentials and issue a signed JWT (FR-001/002).

Request: `{ "email": string, "password": string }`
Responses:
- `200 OK` `{ "token": string, "user": { "id": number, "email": string } }` —
  RS256 JWT (`sub`, `email`, `iss=stocktracker-dev`, `exp`).
- `401 Unauthorized` `code=AUTH_FAILED` — generic message for wrong credentials
  *or* unknown email (FR-002, non-enumerating).
- `403 Forbidden` `code=EMAIL_UNVERIFIED` — account exists but unverified (FR-012);
  body hints to resend verification.
- `429 Too Many Requests` — rate limited (FR-020/SC-006).

## POST /api/auth/forgot-password
Issue a password-reset token (FR-015/016).

Request: `{ "email": string }`
Response: `202 Accepted` `{ "status": "reset_sent" }` — **always identical**
regardless of email existence (FR-016, SC-005).

## POST /api/auth/reset-password
Set a new password via a valid reset token (FR-017/018).

Request: `{ "token": string, "newPassword": string }`
Responses:
- `200 OK` `{ "status": "reset" }` — password updated; existing sessions invalid
  (FR-018).
- `400 Bad Request` `code=TOKEN_INVALID` — unknown/expired/consumed token.
- `400 Bad Request` `code=VALIDATION` — new password fails policy.

## GET /api/auth/me
Return the current authenticated user (used by frontend session bootstrap).

Auth: `Authorization: Bearer <jwt>` required.
Responses: `200 OK` `{ "id": number, "email": string }`; `401` if no/invalid token.

## POST /api/auth/logout
Client discards the token; endpoint is a no-op acknowledgement in dev (stateless
JWT). Response `204 No Content`.

---

## Dev-only token retrieval (FR-T02) — `DevAuthTokenResource`

**Present only when `auth.mode=dev`; MUST NOT exist in production.**

### GET /api/dev/auth/latest-token
Return the most recent unconsumed token for a test email so e2e can drive verify/
reset without an inbox.

Query: `email` (required), `purpose` = `EMAIL_VERIFICATION` | `PASSWORD_RESET`.
Responses:
- `200 OK` `{ "token": string, "purpose": string, "expiresAt": string }`
- `404 Not Found` — no matching unconsumed token.
- `404 Not Found` for the whole route when `auth.mode=cognito` (endpoint absent).

---

## Protected resource behavior (all existing `/api/*` data endpoints)
- Require a valid bearer JWT; otherwise `401 Unauthorized` (FR-004).
- All reads/writes scoped to the JWT subject's `AppUser` (FR-006); a resource id
  owned by another user returns `404 Not Found` (no existence disclosure).
