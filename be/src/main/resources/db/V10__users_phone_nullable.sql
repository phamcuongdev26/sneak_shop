-- Allow seed/admin accounts to exist without a phone number
ALTER TABLE users
  MODIFY COLUMN phone_number VARCHAR(20) NULL;
