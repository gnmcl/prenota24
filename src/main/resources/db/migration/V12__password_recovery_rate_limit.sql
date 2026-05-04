-- Server-side throttling fields for password recovery email sends.
-- Needed to prevent spam even if frontend protections are bypassed.

ALTER TABLE app_user
    ADD COLUMN password_reset_last_sent_at timestamptz,
    ADD COLUMN password_reset_immediate_resend_used boolean NOT NULL DEFAULT false;
