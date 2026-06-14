-- Live market data, multi-currency, extended transactions, alerts.
-- Additive and safe on existing rows: new currency columns default to 'USD',
-- transaction_type widening is app-level (VARCHAR already fits the new values),
-- and the new nullable columns / tables leave all current buy/sell data valid.

-- Native trading currency per instrument (FR-029). On-demand rows (incl. SGX .SI)
-- capture their own currency from the provider.
ALTER TABLE instrument ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Per-user reporting currency (FR-031).
ALTER TABLE app_user ADD COLUMN base_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Cash events have no symbol; dividend/cash movements carry an amount + currency.
-- transaction_type widens to {buy,sell,dividend,split,deposit,withdrawal,fee} at
-- the application layer (the VARCHAR(16) column already accommodates the values).
ALTER TABLE portfolio_transaction MODIFY COLUMN instrument_symbol VARCHAR(16) NULL;
ALTER TABLE portfolio_transaction ADD COLUMN amount DECIMAL(19, 4) NULL;
ALTER TABLE portfolio_transaction ADD COLUMN currency VARCHAR(3) NULL;

-- Latest live quote cache (one row per symbol; upserted by QuoteRefreshJob).
-- Staleness is judged by fetched_at age (provider failing), not market hours.
CREATE TABLE instrument_quote (
  instrument_symbol VARCHAR(16) PRIMARY KEY,
  price DECIMAL(19, 4) NULL,
  change_amount DECIMAL(19, 4) NULL,
  change_pct DECIMAL(9, 4) NULL,
  previous_close DECIMAL(19, 4) NULL,
  as_of TIMESTAMP NULL,
  fetched_at TIMESTAMP NULL,
  source VARCHAR(32) NOT NULL,
  stale BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_instrument_quote_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol)
);

-- Daily exchange rates (cached by FxRefreshJob, read by CurrencyService).
CREATE TABLE fx_rate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  base_currency VARCHAR(3) NOT NULL,
  quote_currency VARCHAR(3) NOT NULL,
  rate_date DATE NOT NULL,
  rate DECIMAL(19, 8) NOT NULL,
  source VARCHAR(32) NOT NULL,
  stale BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uq_fx_rate_pair_date UNIQUE (base_currency, quote_currency, rate_date)
);

-- Price alerts (US4; table created here so V4 is the single additive migration).
CREATE TABLE alert (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  instrument_symbol VARCHAR(16) NOT NULL,
  condition_type VARCHAR(16) NOT NULL,
  threshold DECIMAL(19, 4) NOT NULL,
  armed BOOLEAN NOT NULL DEFAULT TRUE,
  last_triggered_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES app_user(id),
  CONSTRAINT fk_alert_instrument FOREIGN KEY (instrument_symbol) REFERENCES instrument(symbol)
);
CREATE INDEX idx_alert_user ON alert (user_id);

-- In-app notifications (US4).
CREATE TABLE notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  alert_id BIGINT NULL,
  message VARCHAR(255) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_user(id),
  CONSTRAINT fk_notification_alert FOREIGN KEY (alert_id) REFERENCES alert(id) ON DELETE SET NULL
);
CREATE INDEX idx_notification_user ON notification (user_id);
