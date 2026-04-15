-- Add email verification columns to app_user
ALTER TABLE app_user ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE app_user ADD COLUMN verification_code VARCHAR(6);
ALTER TABLE app_user ADD COLUMN verification_code_expires_at TIMESTAMPTZ;

-- Mark all existing users as verified (they registered before this feature)
UPDATE app_user SET email_verified = true;
