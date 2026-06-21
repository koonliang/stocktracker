# Quickstart: Social Login Dev Auth Profile

How to exercise the planned non-production social profile and verify the feature
once implemented.

## Modes

- **dev**: enhanced local/non-production flow with email/password, social
  identity, demo users, and app-issued JWT sessions
- **cognito**: existing production-style Hosted UI flow, unchanged

## Local development baseline

Use the current stack first to verify nothing regresses in `dev` mode:

```bash
docker compose up -d --wait
cd backend && ./mvnw -B test-compile
cd ../frontend && npm run typecheck && npm run build
```

## Non-production social profile setup

Example environment shape for the frontend:

```bash
VITE_AUTH_MODE=dev
VITE_NONPROD_GOOGLE_AUTH_URL=<google-authorization-url>
VITE_NONPROD_FACEBOOK_AUTH_URL=<facebook-authorization-url>
VITE_NONPROD_SOCIAL_REDIRECT_URI=https://<nonprod-origin>/auth/callback
VITE_ENABLE_VERCEL_ANALYTICS=true
VITE_ENABLE_VERCEL_SPEED_INSIGHTS=true
```

Example environment shape for the backend:

```bash
STOCKTRACKER_AUTH_MODE=dev
NONPROD_GOOGLE_CLIENT_ID=<google-client-id>
NONPROD_GOOGLE_CLIENT_SECRET=<google-client-secret>
NONPROD_FACEBOOK_CLIENT_ID=<facebook-client-id>
NONPROD_FACEBOOK_CLIENT_SECRET=<facebook-client-secret>
NONPROD_SOCIAL_REDIRECT_URI=https://<nonprod-origin>/auth/callback
STOCKTRACKER_DEMO_USERS_ENABLED=true
STOCKTRACKER_DEMO_USER_MAX=3
```

## Manual verification checklist

### Social sign-in

1. Open the non-production login page.
2. Confirm the page shows Google, Facebook, and demo-user controls instead of an immediate redirect.
3. Complete a first-time social sign-in.
4. Confirm the app lands on an authenticated route without a separate email verification step.
5. Sign out and repeat the same provider sign-in to confirm the same account is reused.

### Standard dev sign-up

1. Create a new email/password account in `dev` mode.
2. Confirm the account is immediately treated as verified and usable.
3. Confirm sign-in succeeds without any email verification or token retrieval step.

### Demo users

1. Create demo users until three exist.
2. Confirm each created demo user signs in immediately.
3. Confirm each demo user is clearly labeled as a demo account and already owns
   seeded sample data similar to the existing seed user.
4. Attempt to create a fourth demo user and confirm a clear limit message is shown.
5. Select an existing demo user and confirm sign-in succeeds without a password step.

### MVP frontend review

1. Open the User Story 1 mockup artifact.
2. Confirm Google and Facebook are represented as icon actions rather than
   full provider buttons.
3. Confirm the mockup shows the email/password form, social icons, and demo-user
   area on one screen.
4. Confirm the non-production label and visual hierarchy are clear enough for
   frontend validation before backend implementation.

### Telemetry

1. Load the login page and one authenticated page in a non-production Vercel deployment.
2. Confirm Vercel Analytics records page visits for those routes.
3. Confirm Vercel Speed Insights reports page-performance data for the same deployment.
4. Block telemetry scripts in the browser and confirm sign-in plus navigation still work.

## Automated verification targets

```bash
# Backend
cd backend && ./mvnw -B verify

# Frontend
cd frontend && npm run test && npm run typecheck && npm run build

# e2e
mvn -B -f e2e/pom.xml test
```

## Expected automated coverage

- backend endpoint tests for social exchange success/failure
- backend provider-client tests for code exchange/profile lookup with stubs
- backend service tests for demo-user cap and slot allocation
- frontend tests for non-production auth-hub rendering, icon-based social entry,
  and error states
- e2e journey coverage for demo-user creation/login and non-production auth-screen behavior

## Quality gates

- Automated tests covering the feature are created and committed.
- Manual verification is recorded against the checklist above.
- Backend, frontend, and e2e compile/type-check successfully with zero errors.
