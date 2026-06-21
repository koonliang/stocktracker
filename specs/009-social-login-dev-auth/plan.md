# Implementation Plan: Social Login Dev Auth Profile

**Branch**: `009-social-login-dev-auth` | **Date**: 2026-06-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/009-social-login-dev-auth/spec.md`

## Summary

Add a dedicated non-production authentication experience that supports Google/Facebook
social sign-in, skips standalone email verification for first-time non-production
access, provides quick demo-account entry capped at three demo users, and enables
Vercel Analytics plus Vercel Speed Insights for non-production deployments.

The design extends the existing auth stack instead of replacing it:

- **Local dev mode** remains the backend-owned auth flow and is enhanced with
  Google/Facebook social sign-in, auto-verified account creation for all dev
  users, and demo-user
  entry points.
- **Production cognito mode** remains the current Hosted UI + Cognito-token path.

This keeps protected API handling simple, avoids dual-issuer session validation on
every request, and isolates non-production shortcuts from production behavior.

## Technical Context

**Language/Version**: Java 21 (Quarkus 3.15.2 backend); TypeScript 5.6 / React 18.3 (Vite 5.4 frontend); Java 21 (Selenium e2e)  
**Primary Dependencies**: Existing Quarkus auth stack (`quarkus-smallrye-jwt`, `quarkus-security`, Hibernate Panache, Flyway); Quarkus REST client support for Google/Facebook token exchange and profile lookup in `dev` mode; existing React Router 6, Zustand, React Hook Form, and current icon system; add `@vercel/analytics` and `@vercel/speed-insights` to the frontend  
**Storage**: MySQL 8.4 via Hibernate ORM Panache + Flyway for app users, social identities, demo-user metadata, and seeded demo-owned portfolio data; no extra auth-mode storage split  
**Testing**: Backend JUnit 5 + `@QuarkusTest`; frontend Vitest + Testing Library; Selenium e2e for non-production auth journeys; manual verification for Vercel Analytics and Speed Insights event capture plus a User Story 1 MVP mockup review  
**Target Platform**: Local dev stack, Vercel-hosted non-production frontend deployments, Quarkus backend deployments for non-production and production  
**Project Type**: Web application with backend API, SPA frontend, e2e module, and supporting infra/config contracts  
**Performance Goals**: Social or demo sign-in reaches an authenticated app session in under 60 seconds for 95% of non-production attempts; telemetry must not materially delay initial page interaction or block auth flows; the User Story 1 auth screen must be reviewable as an MVP mockup before backend completion  
**Constraints**: Non-production social auth must not depend on Cognito and must reuse the existing `dev` mode instead of introducing a new auth mode; production must not expose demo-user shortcuts or auto-verified dev-mode behavior; protected API handling must stay consistent after sign-in; demo-user count is capped at three per environment; Vercel telemetry integration must degrade safely when blocked or unavailable  
**Scale/Scope**: One enhanced `dev` auth mode for non-production use, up to three reusable demo users per environment, two social providers (Google/Facebook), one shared frontend telemetry integration path, one MVP frontend mockup for User Story 1

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Automated Tests & Manual Verification (NON-NEGOTIABLE)**: PASS — plan adds backend coverage for direct provider OAuth exchange and demo-user rules, frontend coverage for the non-production auth screen, Selenium coverage for demo sign-in and mocked social-auth happy paths, and manual verification steps for telemetry visibility plus MVP mockup review.
- **II. Compilation Integrity (NON-NEGOTIABLE)**: PASS — backend Maven compile/test flows, frontend `tsc --noEmit` + Vite build, and e2e Maven compile remain the verification surface.
- **III. Simplicity & YAGNI**: PASS — the design reuses the current auth stack and extends the existing `dev` mode instead of adding a third auth mode or teaching every protected endpoint to accept multiple session types directly.
- **IV. Specification-Driven Development**: PASS — the plan traces directly to spec `009-social-login-dev-auth`, including the later clarification that User Story 3 must use Vercel's built-in Analytics and Speed Insights components.

Post-design re-check: Phase 1 artifacts preserve the same boundaries. The only added complexity is a direct-social-auth extension inside the existing `dev` mode, which is justified by the need to support both demo sign-in and social sign-in without weakening production constraints.

## Project Structure

### Documentation (this feature)

```text
specs/009-social-login-dev-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── frontend-auth-experience.md
│   ├── nonprod-auth-api.md
│   ├── nonprod-runtime-config.md
│   └── user-story-1-mockup.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/stocktracker/
│   ├── api/
│   │   └── AuthResource.java              # extend with non-production social/demo endpoints
│   ├── bootstrap/
│   │   └── DevDataBootstrap.java          # may seed baseline demo data/users
│   ├── client/                            # provider token/profile REST clients
│   ├── config/                            # dev-mode social auth config helpers
│   ├── domain/
│   │   ├── AppUser.java                   # extend with demo-account metadata
│   │   └── SocialIdentity.java            # reused for social-provider linkage
│   ├── dto/                               # new exchange/demo request-response records
│   ├── security/
│   │   └── AuthMode.java                  # extend mode/profile selection
│   └── service/
│       ├── AccountLinkingService.java     # reused for verified-email linking
│       ├── AuthService.java               # reused for local token issuance
│       ├── DemoUserService.java           # new demo account catalog + login rules
│       └── NonProdSocialAuthService.java  # direct provider OAuth -> app session
├── src/main/resources/
│   ├── application.properties             # non-production auth/profile flags
│   └── db/migration/                      # add demo-user metadata migration
└── src/test/java/com/stocktracker/
    ├── api/                               # social exchange + demo-user endpoint tests
    ├── service/                           # demo-user and social-linking tests
    └── support/

frontend/
├── src/
│   ├── App.tsx                            # mount Vercel Analytics/Speed Insights
│   ├── api/
│   │   └── authApi.ts                     # social callback/demo endpoints
│   ├── auth/
│   │   ├── AuthProvider.tsx               # non-production social callback + demo login
│   │   └── authConfig.ts                  # extend existing dev/cognito config
│   ├── components/auth/                   # social/demo auth entry UI with provider icons
│   └── routes/
│       ├── AuthCallbackRoute.tsx          # exchange provider auth code for app JWT
│       ├── LoginRoute.tsx                 # enhanced dev-mode auth hub
│       └── SignupRoute.tsx                # non-production sign-up/social handoff
├── package.json                           # add Vercel telemetry packages
└── .env.example                           # document non-production auth/telemetry vars

e2e/
└── src/test/java/com/stocktracker/e2e/
    ├── journeys/AuthJourneyTest.java      # extend with demo/non-production path
    ├── pages/LoginPage.java               # new social/demo auth controls
    └── support/                           # test helpers for mocked provider flows
```

**Structure Decision**: This is a web-application feature extending the existing
`backend/`, `frontend/`, and `e2e/` modules. The backend owns app-session issuance,
provider OAuth exchange, and demo-user governance inside the existing `dev` mode;
the frontend owns the enhanced dev-mode sign-in experience and Vercel telemetry
mounting. No infra or Cognito changes are required for this feature.

## Phase 0: Research

Research outcomes are captured in [research.md](./research.md) and resolve the
main design choices:

1. Extend the existing `dev` auth mode so it can exchange Google/Facebook OAuth
   results directly in the backend and issue the app's own JWT session.
2. Auto-verify all newly created standard dev-mode accounts so email
   verification is skipped consistently across non-demo and social sign-up.
3. Represent demo users as first-class passwordless `AppUser` records with
   demo-specific metadata, seeded data similar to the existing seed user, and a
   hard three-account cap per environment.
4. Use Google/Facebook icon affordances in the dev-mode auth hub instead of
   full-width provider buttons.
5. Mount Vercel Analytics and Vercel Speed Insights at the frontend app root and
   gate them with non-production deployment configuration so telemetry never
   blocks auth flows.

## Phase 1: Design & Contracts

Artifacts generated in this phase:

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [quickstart.md](./quickstart.md)
- [contracts/nonprod-auth-api.md](./contracts/nonprod-auth-api.md)
- [contracts/frontend-auth-experience.md](./contracts/frontend-auth-experience.md)
- [contracts/nonprod-runtime-config.md](./contracts/nonprod-runtime-config.md)
- [contracts/user-story-1-mockup.md](./contracts/user-story-1-mockup.md)

Agent context updated: `AGENTS.md` now points at `specs/009-social-login-dev-auth/plan.md`.

## Complexity Tracking

> No Constitution Check violations; section intentionally empty.
