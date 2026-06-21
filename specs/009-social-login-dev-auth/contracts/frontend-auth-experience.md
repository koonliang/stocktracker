# Contract: Frontend Non-Production Auth Experience

Defines how the SPA presents sign-in choices when the existing `dev` mode is
enhanced with social identity and demo users.

## Modes

The frontend auth selector remains a two-way behavior model:

- `dev`: enhanced local/non-production auth with email/password, Google/Facebook,
  and demo users
- `cognito`: existing production-style Hosted UI redirect

## Route Behavior

### `/login`

**dev**
- render an auth hub with:
  - existing email/password sign-in
  - Google icon action
  - Facebook icon action
  - current demo-user list
  - create-demo-user action when fewer than three demo users exist

**cognito**
- keep current immediate redirect behavior

### `/signup`

**dev**
- keeps existing email/password sign-up support
- may render social-provider and demo-user shortcuts, but must not send the user
  through a separate email-verification expectation for any first-time dev-mode
  account creation

### `/auth/callback`

**dev**
- read `code` and provider context from the provider redirect
- call backend social-exchange endpoint
- store the returned app JWT
- navigate to the originally requested route

## UI States

The login surface must support:

- initial loading of available demo users
- empty demo-user list with create action available
- three-user limit reached with clear message
- provider sign-in failure
- passwordless demo-user selection
- telemetry unavailable without blocking interaction

## Stable Test Hooks

Add or preserve test ids for:

- `social-login-google`
- `social-login-facebook`
- `social-icon-google`
- `social-icon-facebook`
- `demo-user-list`
- `demo-user-create`
- `demo-user-login-1`
- `demo-user-login-2`
- `demo-user-login-3`
- `demo-user-limit-message`
- `nonprod-auth-banner`

These complement existing auth journey hooks rather than replacing them.

## Session Rules

- Successful social exchange and successful demo login both end with the same
  local app session shape in `authStore`.
- Sign-out clears the local session and returns the user to the enhanced `dev`
  auth screen.
- The production Cognito callback path must remain intact and must not be
  broken by the `dev`-mode enhancements.

## Telemetry Placement

- Mount Vercel Analytics and Vercel Speed Insights at the app root so they cover
  both the enhanced `dev` auth hub and authenticated routes.
- Telemetry mounting must not block route rendering or sign-in button
  interaction.

## MVP Mockup Expectations

- User Story 1's MVP should be available as a static mockup before backend
  completion.
- The MVP should emphasize:
  - the existing email/password form as the primary action
  - compact Google/Facebook icon entry rather than full provider buttons
  - a clearly separated demo-user area
  - a visible dev/non-production environment label
