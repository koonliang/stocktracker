# Contract: Non-Production Auth API

Defines the backend API additions needed to enhance the existing `dev`-mode
`/api/auth/*` surface with social identity and demo users.

## Purpose

- Start and complete Google/Facebook sign-in for an app-issued session
- List and create demo users
- Auto-login into an existing demo user

Production must not expose the demo-user endpoints or `dev`-mode social exchange
behavior.

## Endpoints

### `POST /api/auth/social/{provider}/exchange`

Accepts the non-production Google/Facebook OAuth callback result and returns the
app's JWT session plus resolved user.

**Request**

```json
{
  "code": "provider-authorization-code",
  "redirectUri": "https://preview.example.com/auth/callback"
}
```

**Response `200 OK`**

```json
{
  "token": "app-jwt",
  "user": {
    "id": 42,
    "email": "user@example.com"
  }
}
```

**Rules**

- Valid only when the backend is running in enhanced `dev` mode.
- `{provider}` is `google` or `facebook`.
- The backend exchanges the authorization code directly with the provider and
  resolves the provider profile server-side.
- A new account created from this flow is immediately active and verified for
  app use.
- A cancelled, expired, or invalid provider result returns an auth failure
  without creating a session.

### `GET /api/auth/demo-users`

Returns the current demo-user catalog for the environment.

**Response `200 OK`**

```json
{
  "users": [
    {
      "slot": 1,
      "label": "Demo User 1",
      "email": "demo1@stocktracker.local"
    }
  ],
  "maxUsers": 3,
  "canCreate": true
}
```

**Rules**

- Visible only in the enhanced `dev` mode.
- The response must be sufficient for the login screen to show current demo
  choices and whether creation is allowed.
- Demo users are passwordless and are not returned as email/password sign-in
  targets.

### `POST /api/auth/demo-users`

Creates the next available demo user and immediately returns an authenticated app
session for it.

**Request**

```json
{
  "label": "Demo User 2"
}
```

`label` may be optional if the backend auto-generates the display name; if the
frontend sends it, the backend validates length and sanitization.

**Response `201 Created`**

```json
{
  "token": "app-jwt",
  "user": {
    "id": 77,
    "email": "demo2@stocktracker.local"
  },
  "demoUser": {
    "slot": 2,
    "label": "Demo User 2"
  }
}
```

**Response `409 Conflict`**

```json
{
  "code": "DEMO_USER_LIMIT_REACHED",
  "message": "The maximum of 3 demo users already exists."
}
```

### `POST /api/auth/demo-users/{slot}/login`

Signs in with an existing demo user and returns a normal app session.

**Response `200 OK`**

```json
{
  "token": "app-jwt",
  "user": {
    "id": 77,
    "email": "demo2@stocktracker.local"
  }
}
```

**Rules**

- `slot` must be `1`, `2`, or `3`
- missing demo user returns `404`
- success path updates `last_login_at` and any optional demo activation audit fields
- no password or credential verification is performed for demo-user login

## Error Model

Use the existing API error envelope style:

```json
{
  "code": "AUTH_FAILED",
  "message": "Unable to complete sign-in."
}
```

Expected error cases:

- invalid/non-production-disabled social exchange
- unsupported provider
- demo-user limit reached
- unknown demo slot
- malformed request payload

## Compatibility Rules

- Existing dev email/password endpoints remain available and gain these
  additional social/demo capabilities.
- Dev-mode sign-up should create active, verified accounts immediately rather
  than issuing a verification step.
- Existing production Cognito flows remain unchanged.
- Protected routes continue to use the app's existing bearer token model after
  successful non-production exchange or demo login.
