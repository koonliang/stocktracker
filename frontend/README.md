# StockTracker Frontend

React/Vite frontend for the integrated StockTracker product. The dashboard,
watchlists, analysis view, and CSV import/export flows now call the Quarkus
backend instead of storing portfolio state in `localStorage`.

For the full-stack workflow and per-story verification steps, see
[`specs/002-connect-frontend-backend/quickstart.md`](../specs/002-connect-frontend-backend/quickstart.md).

## Local development

Run the full product from the repository root:

```sh
docker compose up --build
```

The frontend is served at `http://localhost:5173` and targets
`VITE_API_BASE_URL`, which defaults to `http://localhost:8080/api` in the local
Compose stack.

## Authentication

All app routes are gated behind a session; public auth routes live under
`src/routes/` (`/login`, `/signup`, `/verify-email`, `/forgot-password`,
`/reset-password`, `/signed-out`) and the session lives in `src/stores/authStore.ts`. The
`AuthProvider` (`src/auth/`) selects a strategy from `VITE_AUTH_MODE`:

- **dev** (default): calls the backend `/api/auth/*` flows; the bearer token is
  attached to every API request and a `401` clears the session. The dev-only
  endpoint `GET /api/dev/auth/latest-token` exposes the latest verification/reset
  token so flows can be driven without a live inbox.
- **cognito** (production): redirects unauthenticated users straight to the
  Cognito Hosted UI for login/signup/social/reset and exchanges the auth code on
  return. Configure with:

  ```sh
  VITE_AUTH_MODE=cognito
  VITE_COGNITO_DOMAIN=<hosted-ui-domain>
  VITE_COGNITO_CLIENT_ID=<app-client-id>
  # VITE_COGNITO_REDIRECT_URI defaults to <origin>/auth/callback
  # VITE_COGNITO_LOGOUT_URI defaults to <origin>/signed-out
  ```

The non-production social identity flow lives entirely under
`VITE_AUTH_MODE=dev`. `/login` renders an auth hub with the email/password form,
Google and Facebook provider entry, and passwordless demo-user access. Provider
redirects return to `/auth/callback`, where `AuthProvider` exchanges the
provider code with the backend social endpoint and stores the resulting app JWT
in the normal auth store. The frontend never exchanges provider codes directly
with Google or Facebook.

Demo users share the same dev-mode session model. The login page lists existing
demo accounts, supports creating up to three per environment, and signs in
immediately after creation or selection. Once all three slots exist, the create
control is disabled and the existing demo users remain reusable.

Production `cognito` mode is intentionally unchanged by this feature. The app
still redirects to the Hosted UI immediately in that mode; the non-production
social buttons, demo-user panel, and backend-owned social callback exchange are
all dev-mode only.

Relevant non-production frontend variables:

```sh
VITE_AUTH_MODE=dev
VITE_NONPROD_GOOGLE_AUTH_URL=<google-authorization-url>
VITE_NONPROD_FACEBOOK_AUTH_URL=<facebook-authorization-url>
VITE_NONPROD_SOCIAL_REDIRECT_URI=<origin>/auth/callback
```

The seed dev account is `seed@stocktracker.local` / `DevPass123!` (owns demo
data); `empty@stocktracker.local` / `DevPass123!` is a second verified account
with no data, used by the e2e isolation scenario.

## Quality gates

Run all frontend gates:

```sh
npm test
npm run lint
npm run typecheck
npm run build
```

Frontend-only dev server:

```sh
npm run dev
```
