ALTER TABLE users
    ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER avatar_url;

UPDATE users
SET email_verified = 1
WHERE email IS NOT NULL;
