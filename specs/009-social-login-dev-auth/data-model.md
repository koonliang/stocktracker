# Phase 1 Data Model: Social Login Dev Auth Profile

Maps the feature's key entities to the existing auth schema and the minimal new
metadata required for non-production social sign-in plus passwordless demo-user
access.

## Entities

### AppUser (`app_user`)
The existing application account remains the canonical owner of all protected
data. This feature extends it with demo-account metadata.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK, auto-increment | Existing |
| email | VARCHAR(320) | NOT NULL, UNIQUE | Normalized existing identifier |
| status | ENUM('UNVERIFIED','ACTIVE','LOCKED') | NOT NULL | Existing lifecycle; `dev` mode creates new accounts as `ACTIVE` |
| email_verified | BOOLEAN | NOT NULL | In enhanced `dev` mode, new standard and social accounts are created verified |
| base_currency | VARCHAR(3) | NOT NULL | Existing |
| created_at | TIMESTAMP | NOT NULL | Existing |
| last_login_at | TIMESTAMP | NULL | Existing |
| sessions_invalid_before / sessions_invalid_before_ms | TIMESTAMP / BIGINT | NULL | Existing session invalidation |
| account_kind | ENUM('STANDARD','DEMO') | NOT NULL, default 'STANDARD' | New discriminator for demo users |
| display_name | VARCHAR(120) | NULL | Friendly label shown on non-production auth screen |
| demo_slot | TINYINT | NULL, UNIQUE when not null | Reserved values `1..3` for demo users only |
| demo_last_activated_at | TIMESTAMP | NULL | Optional audit field for demo-user reuse |
| demo_seed_profile | VARCHAR(32) | NULL | Optional seed preset identifier for demo data loading |

- **Identity/uniqueness**:
  - one account per normalized email
  - at most three demo users per environment through unique `demo_slot`
- **Lifecycle**:
  - standard local accounts created in enhanced `dev` mode are created directly as `ACTIVE`
  - social accounts in the non-production profile are created directly as `ACTIVE`
  - demo users are always `ACTIVE`
  - demo users are passwordless and authenticate only through demo-user selection

### SocialIdentity (`social_identity`)
The existing linked-identity table remains the canonical mapping between a
social provider subject and an `AppUser`.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK | Existing |
| user_id | BIGINT | NOT NULL, FK -> app_user(id) | Existing |
| provider | ENUM('GOOGLE','FACEBOOK') | NOT NULL | Existing |
| provider_subject | VARCHAR(255) | NOT NULL | Existing |
| provider_email | VARCHAR(320) | NULL | Existing |
| email_verified | BOOLEAN | NOT NULL | Existing |
| linked_at | TIMESTAMP | NOT NULL | Existing |

- **Behavioral rule**: a verified matching provider email links to an existing
  account; otherwise a new account is created.
- **Non-production rule**: when a new account is created from social sign-in in
  enhanced `dev` mode, the account is immediately marked active and ready for
  app-session issuance.

### Demo User Catalog (derived from `app_user`)
No separate table is required. Demo users are the subset of `app_user` rows
where `account_kind='DEMO'`.

| Attribute | Source | Rule |
|-----------|--------|------|
| slot | `demo_slot` | Must be 1, 2, or 3 |
| label | `display_name` | User-facing name on login screen |
| availability | computed | A slot is selectable when the corresponding row exists |
| sample data ownership | existing user-owned tables | Demo users own seeded dashboard/watchlist/transaction data similar to the existing seed user |
| passwordless access | derived rule | Demo users never require an `auth_credential` row |

### Non-Production Social Exchange (no table)
An application-service flow that receives a Google/Facebook OAuth callback,
exchanges the authorization code with the provider, fetches the provider profile,
and returns the app's own JWT session.

| Field | Type | Notes |
|-------|------|-------|
| provider | enum | Google or Facebook |
| authorization_code | opaque | Temporary input from provider callback |
| resolved_user_id | BIGINT | Result of account linking/provisioning |
| app_session_token | JWT string | App-issued token used after exchange |

Persistence is not required beyond existing `AppUser` and `SocialIdentity`
updates.

### Telemetry Visit Record (external)
No project database table is added for analytics/performance telemetry.

| Attribute | Owner | Notes |
|-----------|-------|-------|
| page visit | Vercel Analytics | Captured by frontend integration |
| speed metrics | Vercel Speed Insights | Captured by frontend integration |

The project only controls whether the components are mounted and whether the app
continues to function if telemetry collection is blocked.

## Relationships

```text
AppUser 1───* SocialIdentity
AppUser(DEMO subset) 1───* Watchlist
AppUser(DEMO subset) 1───* PortfolioTransaction
AppUser(STANDARD/DEMO) ──> app-issued JWT session
Vercel Analytics / Speed Insights ──> external telemetry only
```

## Validation Rules

- `account_kind='DEMO'` requires:
  - `status='ACTIVE'`
  - `email_verified=true`
  - non-null `display_name`
  - non-null `demo_slot` in the range `1..3`
  - no password-based login requirement
- `account_kind='STANDARD'` requires `demo_slot` to be null.
- Standard dev-mode sign-up must persist `status='ACTIVE'` and `email_verified=true`.
- Demo-user creation must fail when three `DEMO` accounts already exist.
- Demo-user login must succeed without checking `auth_credential`.
- Social exchange must reject cancelled/invalid provider results without creating
  a session or duplicate user row.
- Standard and social dev-mode account creation must mark the resulting account
  active immediately so no separate verification token is required.

## Migration Outline

Expected follow-up migration (new Flyway version after `V5`):

1. Add `account_kind`, `display_name`, `demo_slot`, and `demo_last_activated_at`
   columns to `app_user`.
2. Backfill existing users to `account_kind='STANDARD'`.
3. Add a uniqueness constraint for `demo_slot` where present.
4. Seed up to three clearly labeled passwordless demo accounts only when the
   enhanced `dev` mode enables them.
5. Seed each demo account with portfolio/watchlist/transaction data using the
   same bootstrap pattern currently used for the seed user.
