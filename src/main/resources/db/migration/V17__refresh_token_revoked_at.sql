-- Momento della rotazione di un refresh token. Permette di distinguere un rinnovo concorrente
-- legittimo (due schede che presentano lo stesso token entro pochi secondi) da un riuso
-- sospetto. Resta NULL per i token revocati in blocco (logout, cambio password).

ALTER TABLE refresh_token
    ADD COLUMN revoked_at TIMESTAMPTZ;
