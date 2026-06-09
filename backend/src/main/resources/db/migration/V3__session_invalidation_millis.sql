ALTER TABLE app_user
  ADD COLUMN sessions_invalid_before_ms BIGINT NULL AFTER sessions_invalid_before;
