-- Campi dedicati al recupero password, separati dal codice di verifica email.
-- Usare campi distinti evita che un codice di recovery possa essere accettato
-- dall'endpoint verify-email e viceversa.

ALTER TABLE app_user
    ADD COLUMN password_reset_code            varchar(6),
    ADD COLUMN password_reset_code_expires_at timestamptz;

