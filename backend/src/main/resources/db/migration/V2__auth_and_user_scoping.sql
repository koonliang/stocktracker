-- Authentication schema + per-user scoping of watchlist / portfolio_transaction.
-- Instrument reference data stays global. A deterministic seed user (id=1) owns
-- all pre-existing rows so the NOT NULL user_id constraint can be added safely.

CREATE TABLE app_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(320) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP NULL,
  -- Tokens issued before this instant are rejected (password reset / forced logout, FR-018).
  sessions_invalid_before TIMESTAMP NULL,
  CONSTRAINT uq_app_user_email UNIQUE (email)
);

CREATE TABLE auth_credential (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  password_hash VARCHAR(72) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_auth_credential_user UNIQUE (user_id),
  CONSTRAINT fk_auth_credential_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE social_identity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  provider VARCHAR(16) NOT NULL,
  provider_subject VARCHAR(255) NOT NULL,
  provider_email VARCHAR(320) NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_social_identity_provider_subject UNIQUE (provider, provider_subject),
  CONSTRAINT fk_social_identity_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE verification_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  purpose VARCHAR(24) NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_verification_token_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_verification_token_user_purpose ON verification_token (user_id, purpose);
CREATE INDEX idx_verification_token_hash ON verification_token (token_hash);

-- Seed user that owns all pre-existing shared data.
INSERT INTO app_user (id, email, status, email_verified, created_at)
VALUES (1, 'seed@stocktracker.local', 'ACTIVE', TRUE, CURRENT_TIMESTAMP);

-- Scope watchlist to its owner.
ALTER TABLE watchlist ADD COLUMN user_id BIGINT NULL;
UPDATE watchlist SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE watchlist MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE watchlist ADD CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES app_user(id);
CREATE INDEX idx_watchlist_user ON watchlist (user_id);

-- A watchlist name is unique per owner (replaces the global unique-name index).
DROP INDEX uq_watchlist_name_lower ON watchlist;
CREATE UNIQUE INDEX uq_watchlist_user_name_lower ON watchlist (user_id, (LOWER(name)));

-- Scope portfolio_transaction to its owner.
ALTER TABLE portfolio_transaction ADD COLUMN user_id BIGINT NULL;
UPDATE portfolio_transaction SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE portfolio_transaction MODIFY COLUMN user_id BIGINT NOT NULL;
ALTER TABLE portfolio_transaction ADD CONSTRAINT fk_portfolio_tx_user FOREIGN KEY (user_id) REFERENCES app_user(id);
CREATE INDEX idx_portfolio_tx_user ON portfolio_transaction (user_id);
