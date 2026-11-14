-- Eliminare un cliente cancellava a cascata tutti i suoi appuntamenti (storico e agenda futura).
-- Il servizio ora rifiuta l'eliminazione se esistono appuntamenti; il vincolo lo garantisce
-- anche per qualsiasi altro percorso.

ALTER TABLE appointment
    DROP CONSTRAINT fk_appointment_client,
    ADD CONSTRAINT fk_appointment_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE RESTRICT;
