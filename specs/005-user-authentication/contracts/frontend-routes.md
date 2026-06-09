# Contract: Frontend Auth Routes & Session Handling

Extends the existing React Router 6 / Zustand / fetch-wrapper app. New code lives
under `frontend/src/auth/`, `frontend/src/routes/`, `frontend/src/stores/`.

## Routes

| Path | Component | Access | Purpose |
|------|-----------|--------|---------|
| `/login` | `LoginRoute` | Public | Email+password sign-in; links to signup/forgot; social buttons (prod) |
| `/signup` | `SignupRoute` | Public | Create account → "check your email" state |
| `/verify-email` | `VerifyEmailRoute` | Public | Consumes `?token=`; on success routes to `/login` |
| `/forgot-password` | `ForgotPasswordRoute` | Public | Request reset; always shows neutral confirmation |
| `/reset-password` | `ResetPasswordRoute` | Public | Consumes `?token=`; set new password |
| `/`, `/watchlists`, `/watchlists/:id`, `/transactions`, `/analysis/:ticker` | existing | **Protected** | Wrapped by `ProtectedRoute` |

`App.tsx` renders public auth routes outside the `AppShell`, and wraps the
existing shell/routes in `<ProtectedRoute>`.

## ProtectedRoute
- If no valid session → redirect to `/login` (preserving intended path) (FR-004).
- On mount with a stored token, calls `GET /api/auth/me` (dev) / validates Cognito
  session to hydrate `authStore`.

## authStore (Zustand)
State: `{ token: string|null, user: {id,email}|null, status: 'anonymous'|'authenticated'|'loading' }`.
Actions: `login(email,password)`, `logout()`, `hydrate()`, `setSession(token,user)`.
Token kept in memory + mirrored to `sessionStorage` (cleared on logout / 401).

## API client changes (`client.ts`)
- Attach `Authorization: Bearer <token>` from `authStore` to every request.
- On `401`: clear `authStore`, redirect to `/login` (single retry suppressed for
  auth failures).

## AuthProvider strategy
A thin abstraction selected by build/env config:
- **dev strategy**: calls `/api/auth/*` (auth-api.md); used by e2e and local dev.
- **cognito strategy**: Cognito Hosted UI redirect for login/signup/social/reset;
  handles the auth-code callback and stores the resulting JWT.

Only the dev strategy is exercised by the browser e2e journey (clarify decision).

## Stable test hooks (FR-T03)
Add `data-testid` attributes consumed by the e2e page objects, at minimum:
`login-email`, `login-password`, `login-submit`, `login-error`,
`signup-email`, `signup-password`, `signup-submit`, `signup-confirmation`,
`verify-status`, `forgot-email`, `forgot-submit`, `reset-password`,
`reset-submit`, `logout-button`, and an authenticated-shell marker
`app-shell-authenticated`.

## Validation / UX
- Inline password-policy feedback on signup/reset (FR-010).
- Generic, non-enumerating error copy on login/forgot (FR-002/016, SC-005).
- Loading and error states on every form (spec edge cases).
