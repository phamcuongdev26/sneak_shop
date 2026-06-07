ALTER TABLE users
  ADD COLUMN lock_reason TEXT NULL AFTER deleted_at;
