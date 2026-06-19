-- Alert notifications and currency dashboard refinements.
-- Adds notification trigger snapshots, crossing dedup, alert re-arm tracking,
-- transaction currency source tracking, and legacy backfill support.
-- Safe on existing rows: new columns are nullable or have defaults.

-- Track re-arm state and condition history on alerts.
ALTER TABLE alert ADD COLUMN last_condition_met BOOLEAN NULL;
ALTER TABLE alert ADD COLUMN last_cleared_at TIMESTAMP NULL;

-- Enrich notifications with trigger snapshot fields for history display.
ALTER TABLE notification ADD COLUMN instrument_symbol VARCHAR(16) NULL;
ALTER TABLE notification ADD COLUMN condition_type VARCHAR(16) NULL;
ALTER TABLE notification ADD COLUMN threshold DECIMAL(19, 4) NULL;
ALTER TABLE notification ADD COLUMN observed_value DECIMAL(19, 4) NULL;
ALTER TABLE notification ADD COLUMN observed_currency VARCHAR(3) NULL;
ALTER TABLE notification ADD COLUMN triggered_at TIMESTAMP NULL;
ALTER TABLE notification ADD COLUMN crossing_key VARCHAR(64) NULL;
ALTER TABLE notification ADD COLUMN updated_at TIMESTAMP NULL;

-- Backfill triggered_at from created_at for existing rows.
UPDATE notification SET triggered_at = created_at WHERE triggered_at IS NULL;

-- Deduplicate notifications per alert crossing.
CREATE UNIQUE INDEX uq_notification_alert_crossing ON notification (alert_id, crossing_key);

-- Drop old FK and recreate with CASCADE so alert deletion removes notification history.
ALTER TABLE notification DROP FOREIGN KEY fk_notification_alert;
ALTER TABLE notification ADD CONSTRAINT fk_notification_alert
  FOREIGN KEY (alert_id) REFERENCES alert(id) ON DELETE CASCADE;

-- Index for newest-first notification queries.
CREATE INDEX idx_notification_user_triggered ON notification (user_id, triggered_at DESC);

-- Track currency source and backfill state on transactions.
ALTER TABLE portfolio_transaction ADD COLUMN currency_source VARCHAR(32) NULL;
ALTER TABLE portfolio_transaction ADD COLUMN currency_backfilled_at TIMESTAMP NULL;

-- Index for finding legacy transactions missing currency.
CREATE INDEX idx_transaction_currency ON portfolio_transaction (user_id, currency);
