# Contract: Non-Production Runtime Configuration

Defines the configuration surface required to enhance the existing `dev` mode
with social auth and demo users, plus Vercel telemetry integration.

## Backend Runtime Values

| Setting | Purpose | Expected Source |
|--------|---------|-----------------|
| `STOCKTRACKER_AUTH_MODE` | Remains `dev` for the enhanced non-production path | backend deployment env |
| `NONPROD_GOOGLE_CLIENT_ID` | Google OAuth client id for non-production social login | backend deployment env |
| `NONPROD_GOOGLE_CLIENT_SECRET` | Google OAuth client secret for backend code exchange | backend deployment env |
| `NONPROD_FACEBOOK_CLIENT_ID` | Facebook OAuth client id for non-production social login | backend deployment env |
| `NONPROD_FACEBOOK_CLIENT_SECRET` | Facebook OAuth client secret for backend code exchange | backend deployment env |
| `NONPROD_SOCIAL_REDIRECT_URI` | Redirect URI used by provider code exchange | backend deployment env |
| `STOCKTRACKER_DEMO_USERS_ENABLED` | Enables demo-user endpoints and creation rules | backend deployment env |
| `STOCKTRACKER_DEMO_USER_MAX` | Demo-user cap; defaults to `3` | backend deployment env |
| `STOCKTRACKER_DEMO_USER_PREFIX` | Optional email/name prefix for demo-user generation | backend deployment env |

## Frontend Runtime Values

| Setting | Purpose | Expected Source |
|--------|---------|-----------------|
| `VITE_AUTH_MODE` | Remains `dev` for the enhanced non-production path | frontend build env |
| `VITE_NONPROD_GOOGLE_AUTH_URL` | Google authorization URL for non-production sign-in | frontend build env or derived config |
| `VITE_NONPROD_FACEBOOK_AUTH_URL` | Facebook authorization URL for non-production sign-in | frontend build env or derived config |
| `VITE_NONPROD_SOCIAL_REDIRECT_URI` | Callback path used after provider sign-in | frontend build env |
| `VITE_ENABLE_VERCEL_ANALYTICS` | Enables Analytics component for non-production deployments | frontend build env |
| `VITE_ENABLE_VERCEL_SPEED_INSIGHTS` | Enables Speed Insights component for non-production deployments | frontend build env |

## Behavioral Rules

- Production deployments must continue using the current Cognito-only path and
  must not enable demo-user APIs.
- Local developer environments may keep using `dev` auth mode by default.
- The non-production enhancement must reuse `dev` mode rather than introducing a
  new auth-mode value.
- Vercel Analytics and Speed Insights may be mounted conditionally, but their
  absence must not break page rendering or auth flows.

## Validation Rules

- `STOCKTRACKER_DEMO_USER_MAX` must be a positive integer and defaults to `3`.
- If `STOCKTRACKER_AUTH_MODE=dev` is being used for non-production social sign-in,
  the provider authorization URLs, backend provider client credentials, and
  redirect URI must be present.
- Telemetry flags may be false or omitted without blocking the app.

## Compatibility Rules

- Existing `frontend/.env.example` and backend config docs should be updated to
  document the enhanced `dev` mode without changing current defaults.
- No production-only Cognito secret or callback URL should be reused implicitly
  for the non-production social profile.
