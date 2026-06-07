ALTER TABLE users
    ADD COLUMN username VARCHAR(100) NULL AFTER email,
    ADD COLUMN gender ENUM('MALE','FEMALE','OTHER') NULL AFTER avatar_url,
    ADD COLUMN birth_date DATE NULL AFTER gender;

CREATE UNIQUE INDEX idx_users_username ON users (username);
