-- Ensure users.enabled exists with a safe default for seeded accounts and legacy rows
ALTER TABLE users
  MODIFY COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1;

UPDATE users
SET enabled = 1
WHERE enabled IS NULL;
