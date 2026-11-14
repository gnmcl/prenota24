-- Contatori dei tentativi falliti sui codici OTP a 6 cifre (verifica email e reset password).
-- Senza un limite per account i codici si possono indovinare per forza bruta
-- distribuendo le richieste su più IP. Al raggiungimento del limite il codice viene invalidato.

ALTER TABLE app_user
    ADD COLUMN verification_failed_attempts integer NOT NULL DEFAULT 0,
    ADD COLUMN password_reset_failed_attempts integer NOT NULL DEFAULT 0;
