# Contract: Authentication Regression Journey (e2e)

Realizes spec Story 5 / FR-T03 in the feature-004 `e2e/` Selenium module. Runs
headlessly against docker-compose with `STOCKTRACKER_AUTH_MODE=dev` and
`STOCKTRACKER_DEV_BOOTSTRAP_ENABLED=true`. New test class `AuthJourneyTest`
extending `BaseTest`, new page objects, and `DevTokenClient`.

## New artifacts
- `pages/LoginPage.java`, `pages/SignupPage.java`, `pages/ForgotPasswordPage.java`
  (Page-Object pattern, using `data-testid` from frontend-routes.md).
- `support/DevTokenClient.java` — `java.net.http.HttpClient` wrapper calling
  `GET {backendBaseUrl}/api/dev/auth/latest-token?email=&purpose=` (FR-T02), with
  `-De2e.backendBaseUrl` (default `http://localhost:8080`).
- `journeys/AuthJourneyTest.java`.

## Scenarios (each an `@Test` / `@Step` sequence)

1. **Sign-up → verify → sign-in → protected → sign-out** (happy path)
   - Sign up a unique email; assert confirmation state.
   - `DevTokenClient` fetches the `EMAIL_VERIFICATION` token; visit
     `/verify-email?token=...`; assert verified.
   - Log in; assert authenticated shell (`app-shell-authenticated`) + dashboard.
   - Sign out; assert redirected to `/login`.

2. **Invalid credentials rejected** — wrong password → `login-error` shown, no
   session (FR-002).

3. **Protected-route redirect** — navigate directly to `/transactions` while
   signed out → redirected to `/login` (FR-004).

4. **Password reset** — forgot-password for the verified user → fetch
   `PASSWORD_RESET` token → set new password → sign in with new password succeeds,
   old password rejected (FR-017/018).

5. **Per-user data isolation** — sign in as seeded user A (has data) and seeded
   user B (empty); assert B never sees A's portfolio/watchlist/transactions
   (FR-006/SC-002). Requires bootstrap to seed two verified users.

## Out of scope (this journey)
- Google/Facebook social login — excluded from browser e2e (clarify / FR-T04);
  covered by backend mocked-provider integration tests instead.

## CI integration
- Runs inside the existing `.github/workflows/regression.yml` job
  (`mvn -B -f e2e/pom.xml test`) — no structural workflow change.
- On failure: existing `ScreenshotOnFailure` + Surefire/Allure artifacts apply.
- A failing auth scenario blocks the PR (FR-T05 / SC-010), consistent with the
  current regression gate.

## Pass criteria
All five scenarios green within the suite's <10-minute budget; failure screenshots
produced on any failure.
