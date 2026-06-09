# Phase 1 Data Model: User Authentication & Account Management

Maps spec Key Entities → MySQL schema (Flyway `V2`) and Panache JPA entities.
Reference data (`instrument*`) is unchanged and remains global. Only `watchlist`
and `portfolio_transaction` become user-scoped.

## Entities

### AppUser  (`app_user`)
The account that can sign in; owns all user-scoped data.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK, auto-increment | |
| email | VARCHAR(320) | NOT NULL, UNIQUE | Normalized: trimmed + lowercased |
| status | ENUM('UNVERIFIED','ACTIVE','LOCKED') | NOT NULL, default 'UNVERIFIED' | Lifecycle below |
| email_verified | BOOLEAN | NOT NULL, default false | |
| created_at | TIMESTAMP | NOT NULL | |
| last_login_at | TIMESTAMP | NULL | |

- **Identity/uniqueness**: one account per normalized email (FR-014).
- **Lifecycle**: `UNVERIFIED` → `ACTIVE` (on email verification, FR-013) →
  `LOCKED` (optional, on abuse). Sign-in allowed only when `ACTIVE` (FR-012).
- Social-only accounts have **no** `auth_credential` row (credential optional,
  FR-S07).

### AuthCredential  (`auth_credential`)
Dev-mode password material. Absent for social-only users.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK | |
| user_id | BIGINT | NOT NULL, UNIQUE, FK → app_user(id) ON DELETE CASCADE | 1:1 |
| password_hash | VARCHAR(72) | NOT NULL | BCrypt hash (FR-019) |
| updated_at | TIMESTAMP | NOT NULL | Reset bumps this |

- Never stores plaintext or reversible form (FR-019). Not used in cognito mode
  (Cognito owns credentials).

### SocialIdentity  (`social_identity`)
A linked external provider identity (FR-S03/S07).

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK | |
| user_id | BIGINT | NOT NULL, FK → app_user(id) ON DELETE CASCADE | many:1 |
| provider | ENUM('GOOGLE','FACEBOOK') | NOT NULL | |
| provider_subject | VARCHAR(255) | NOT NULL | Provider-side stable subject id |
| provider_email | VARCHAR(320) | NULL | Email asserted by provider |
| email_verified | BOOLEAN | NOT NULL | Provider's verification flag (FR-S04) |
| linked_at | TIMESTAMP | NOT NULL | |

- **Uniqueness**: `UNIQUE(provider, provider_subject)`.
- One `AppUser` may have several identities (FR-S07). Auto-link only when
  `email_verified=true` and `provider_email` matches an existing account (FR-S03);
  otherwise refuse linking (FR-S04).

### VerificationToken  (`verification_token`)
Single-use, time-limited challenge for verification or reset (FR-013/FR-017).

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | BIGINT | PK | |
| user_id | BIGINT | NOT NULL, FK → app_user(id) ON DELETE CASCADE | |
| purpose | ENUM('EMAIL_VERIFICATION','PASSWORD_RESET') | NOT NULL | |
| token_hash | CHAR(64) | NOT NULL | SHA-256 of opaque token; raw token only emailed |
| expires_at | TIMESTAMP | NOT NULL | Verify e.g. 24h; reset e.g. 1h |
| consumed_at | TIMESTAMP | NULL | Set on first use → single-use (SC-007) |

- Index `(user_id, purpose)`. Issuing a new token of a purpose supersedes prior
  unconsumed ones. A `@Scheduled` job purges expired/consumed rows.

### Session / Auth Token  (no table — stateless JWT)
Represented by the signed JWT (dev-issued or Cognito-issued); not persisted.
Sign-out is client-side token discard; password reset forces re-auth by short
token lifetime + (dev) a `tokens_valid_after` check on `app_user` if needed for
FR-018. Cognito handles global sign-out in prod.

## Modified existing entities

### watchlist  (MODIFIED)
Add `user_id BIGINT NOT NULL, FK → app_user(id)`. Index `(user_id)`. All queries
filter by owner.

### portfolio_transaction  (MODIFIED)
Add `user_id BIGINT NOT NULL, FK → app_user(id)`. Index `(user_id)`. All queries
filter by owner.

`watchlist_item` inherits scope through its parent `watchlist` (no direct column).

## Relationships

```
AppUser 1───0..1 AuthCredential
AppUser 1───*    SocialIdentity      (UNIQUE provider+subject)
AppUser 1───*    VerificationToken
AppUser 1───*    Watchlist           (NEW user_id FK)
AppUser 1───*    PortfolioTransaction(NEW user_id FK)
Instrument*  ... unchanged, global/shared (no user scope)
```

## Migration `V2__auth_and_user_scoping.sql` (outline)

1. `CREATE TABLE app_user, auth_credential, social_identity, verification_token`.
2. Insert deterministic **seed user** (`status='ACTIVE'`, `email_verified=true`,
   e.g. `seed@stocktracker.local`) to own pre-existing data.
3. `ALTER TABLE watchlist ADD COLUMN user_id BIGINT NULL`; backfill = seed user id;
   then `MODIFY ... NOT NULL` + add FK + index. Same for `portfolio_transaction`.
4. (Dev bootstrap may additionally seed a verified demo user + a second user for
   the e2e isolation check.)

## Validation rules (from requirements)

- Email: RFC-style format check + normalization before persistence (FR-010/edge).
- Password: ≥8 chars + documented complexity, enforced at sign-up and reset
  (FR-010/FR-017).
- Token: reject if `consumed_at` set or `expires_at` past (FR-013/FR-017/SC-007).
- Cross-user access: queries scoped to `CurrentUser.id`; foreign id → 404 (FR-006).
- Social link: require provider `email_verified=true` to auto-link (FR-S03/S04).
