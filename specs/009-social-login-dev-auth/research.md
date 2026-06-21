# Research: Social Login Dev Auth Profile

## Decision 1: Extend the existing `dev` mode with backend-owned Google/Facebook OAuth

**Decision**: Extend the existing `dev` mode so it supports backend-owned
Google/Facebook OAuth flows, then issues the app's own JWT before the user
enters protected routes.

**Rationale**:

- The repo already separates local `dev` auth from production `cognito` auth.
- Demo-user auto-login needs the backend to mint an immediate app session without
  forcing an external identity-system admin flow.
- Standard dev users should follow the same no-verification experience as social
  and demo users, avoiding split behavior inside one mode.
- Exchanging social identity into the app's JWT keeps the protected-resource
  layer consistent after sign-in and avoids per-request acceptance of both local
  and external-provider sessions.
- The existing `AccountLinkingService` and `TokenIssuer` can be reused instead of
  building a second protected-route model.
- Reusing `dev` mode avoids inventing a third auth-mode branch and keeps config
  and testing aligned with the current non-production path.

**Alternatives considered**:

- **Reuse Cognito in non-production**: rejected because the user explicitly
  ruled it out and it would couple this feature to infra that is not part of the
  non-production requirement.
- **Create a brand-new non-production auth mode**: rejected because the user
  explicitly wants this implemented as an enhancement to the existing `dev` mode.
- **Allow protected APIs to accept both provider tokens and app-issued tokens in
  one non-production mode**: rejected because it complicates auth validation on
  every request and increases risk of production leakage.
- **Implement direct Google/Facebook OAuth only in the frontend**: rejected
  because provider secrets and token verification belong on the backend, and the
  app already has a backend session model worth reusing.

## Decision 2: Auto-verify all standard dev-mode accounts

**Decision**: Standard email/password accounts created in `dev` mode are marked
active and verified immediately, just like first-time social sign-ins.

**Rationale**:

- The user explicitly stated that non-demo users in `dev` mode should not require
  email verification.
- Keeping one verification policy per mode is simpler than mixing verified and
  unverified account behavior in the same dev environment.
- It removes the need for verification-token retrieval during ordinary dev-mode
  sign-up validation, reducing friction for local and preview testing.

**Alternatives considered**:

- **Keep email/password sign-up unverified while only social/demo users are
  auto-verified**: rejected because it creates inconsistent dev-mode behavior.
- **Remove email/password sign-up entirely from dev mode**: rejected because the
  current auth surface still uses email/password as a primary path.

## Decision 3: Model demo users as regular app users with demo-specific metadata

**Decision**: Store demo users in `app_user` with explicit demo markers such as
account kind, display label, and a unique demo slot capped at three per
environment. Demo users do not require passwords and are seeded with portfolio
data similar to the existing seed user.

**Rationale**:

- Demo sign-in should reuse the same JWT/session, authorization, and data
  isolation behavior as normal users.
- A first-class user record allows demo accounts to own watchlists,
  transactions, and dashboard data without introducing a parallel fake-session
  mechanism.
- A unique `demo_slot` or equivalent cap is simpler to reason about and easier
  to test than an unconstrained set of ad hoc seed accounts.
- Passwordless demo access matches the requested quick-entry workflow and avoids
  maintaining credentials that are never typed.
- Seeded data makes the demo path useful immediately for testing and demos.

**Alternatives considered**:

- **Hardcode three seed accounts only in bootstrap**: rejected because the spec
  requires demo account creation and lifecycle control, not only fixed accounts.
- **Separate `demo_user` table unrelated to `app_user`**: rejected because it
  would duplicate ownership and session-mapping logic already solved by
  `AppUser`.
- **Ephemeral anonymous sessions**: rejected because they do not support clear
  reuse, labeling, or data ownership semantics.
- **Password-protected demo users**: rejected because the requested demo flow is
  passwordless.

## Decision 4: Use provider icons rather than full social buttons on the MVP auth screen

**Decision**: Present Google and Facebook entry points as recognizable icons in
the MVP auth screen instead of full-width labeled provider buttons.

**Rationale**:

- The user explicitly requested icon-based provider entry.
- The screen already contains a primary email/password action, so icon
  affordances reduce visual competition while keeping social entry obvious.
- Icon-based entry fits more cleanly beside the demo-user section in a compact
  MVP layout.

**Alternatives considered**:

- **Full-width social buttons**: rejected because the user explicitly asked for
  icons instead.
- **Text links only**: rejected because they are less scannable and visually
  weaker for first-pass validation.

## Decision 5: Keep Vercel observability integration at the SPA root using Vercel's built-in packages

**Decision**: Add Vercel's built-in Analytics and Speed Insights React
components at the frontend app root and enable them for non-production
deployments through environment-aware configuration.

**Rationale**:

- The user explicitly clarified that User Story 3 must use Vercel's built-in
  Analytics and Speed Insights components.
- The official quickstarts for React show root-level component usage for both
  products, which maps cleanly to the existing `App.tsx` entry surface.
- Root-level mounting ensures sign-in pages and authenticated routes are covered
  by the same telemetry path, while graceful degradation remains a frontend-only
  concern.

**Alternatives considered**:

- **Use custom page-event instrumentation**: rejected because it ignores the
  explicit clarification and adds maintenance with no benefit.
- **Limit telemetry to authenticated routes only**: rejected because the spec
  explicitly includes sign-in flow visibility in User Story 3.
- **Enable telemetry in all environments unconditionally**: rejected because the
  feature scope is non-production observability, and production behavior should
  remain unchanged unless specified separately.

## Decision 6: Present a non-production auth hub instead of auto-redirecting immediately

**Decision**: In enhanced `dev` mode, the login/sign-up entry surfaces show
explicit choices for Google, Facebook, and demo users instead of only the current
email/password form.

**Rationale**:

- Demo-user quick access needs an in-app control surface.
- The current dev-mode form leaves no place to expose demo-user actions or
  social-provider entry points cleanly.
- A visible auth hub supports manual verification of both social and demo flows.

**Alternatives considered**:

- **Keep instant redirect and add a separate demo URL**: rejected because it
  hides one of the core feature capabilities behind a non-discoverable path.
- **Show only social providers on login and hide demo flows elsewhere**:
  rejected because demo-user quick access is a primary story, not an admin-only
  afterthought.
