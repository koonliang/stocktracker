ALTER TABLE app_user
  ADD COLUMN account_kind VARCHAR(16) NOT NULL DEFAULT 'STANDARD',
  ADD COLUMN display_name VARCHAR(120) NULL,
  ADD COLUMN demo_slot TINYINT NULL,
  ADD COLUMN demo_last_activated_at TIMESTAMP NULL,
  ADD COLUMN demo_seed_profile VARCHAR(32) NULL;

CREATE UNIQUE INDEX uq_app_user_demo_slot ON app_user (demo_slot);

