-- Server-side throttling fields for verification email sends.
-- Protects /auth/register retries and /auth/resend-verification from email spam.

ALTER TABLE app_user
    ADD COLUMN verification_last_sent_at timestamptz,
    ADD COLUMN verification_immediate_resend_used boolean NOT NULL DEFAULT false;
