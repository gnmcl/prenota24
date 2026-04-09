-- ====================================================================
-- V3__business_evolution.sql
-- Evoluzione a "Prenota24 for Business"
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. STUDIO: aggiungi slug per URL pubblici di booking
-- --------------------------------------------------------------------
ALTER TABLE studio
    ADD COLUMN slug VARCHAR(100) UNIQUE;

CREATE INDEX idx_studio_slug ON studio(slug);

-- --------------------------------------------------------------------
-- 2. APP_USER: collega utente PROFESSIONAL alla sua entity professional
-- --------------------------------------------------------------------
ALTER TABLE app_user
    ADD COLUMN professional_id UUID,
    ADD CONSTRAINT fk_user_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE SET NULL;

-- Al massimo un app_user per professional
CREATE UNIQUE INDEX idx_appuser_professional
    ON app_user(professional_id)
    WHERE professional_id IS NOT NULL;

-- --------------------------------------------------------------------
-- 3. CLIENT: estendi con campi aggiuntivi
-- --------------------------------------------------------------------
ALTER TABLE client
    ADD COLUMN notes       TEXT,
    ADD COLUMN source      VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN tags        TEXT[],
    ADD COLUMN updated_at  TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE client
    ADD CONSTRAINT chk_client_source
        CHECK (source IN ('MANUAL', 'RESERVATION_IMPORT', 'PUBLIC_BOOKING', 'API'));

CREATE INDEX idx_client_studio ON client(studio_id);
CREATE INDEX idx_client_email  ON client(studio_id, email);

-- --------------------------------------------------------------------
-- 4. RINOMINA booking → appointment
-- --------------------------------------------------------------------
ALTER TABLE booking RENAME TO appointment;

ALTER TABLE appointment RENAME CONSTRAINT fk_booking_studio        TO fk_appointment_studio;
ALTER TABLE appointment RENAME CONSTRAINT fk_booking_professional   TO fk_appointment_professional;
ALTER TABLE appointment RENAME CONSTRAINT fk_booking_client        TO fk_appointment_client;
ALTER TABLE appointment RENAME CONSTRAINT chk_booking_time         TO chk_appointment_time;
ALTER TABLE appointment RENAME CONSTRAINT no_overlapping_bookings  TO no_overlapping_appointments;

-- --------------------------------------------------------------------
-- 5. APPOINTMENT: estendi con nuovi campi
-- --------------------------------------------------------------------
ALTER TABLE appointment
    ADD COLUMN service_type_id     UUID,
    ADD COLUMN notes               TEXT,
    ADD COLUMN proposed_start      TIMESTAMP,
    ADD COLUMN proposed_end        TIMESTAMP,
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN cancelled_by        VARCHAR(50),
    ADD COLUMN token               VARCHAR(64) UNIQUE,
    ADD COLUMN updated_at          TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE appointment
    ADD CONSTRAINT chk_appointment_status
        CHECK (status IN (
            'REQUESTED',
            'CONFIRMED',
            'PROPOSED_NEW_TIME',
            'CANCELLED',
            'COMPLETED',
            'NO_SHOW'
        ));

ALTER TABLE appointment
    ADD CONSTRAINT chk_appointment_cancelled_by
        CHECK (cancelled_by IS NULL OR cancelled_by IN ('CLIENT', 'PROFESSIONAL', 'SYSTEM'));

-- Ridefinisci il constraint EXCLUDE: blocca anche REQUESTED e PROPOSED_NEW_TIME
-- (non solo CONFIRMED come in V1)
ALTER TABLE appointment DROP CONSTRAINT no_overlapping_appointments;

ALTER TABLE appointment
    ADD CONSTRAINT no_overlapping_appointments
        EXCLUDE USING gist (
            professional_id WITH =,
            tsrange(start_datetime, end_datetime, '[)') WITH &&
        )
        WHERE (status IN ('REQUESTED', 'CONFIRMED', 'PROPOSED_NEW_TIME'));

CREATE INDEX idx_appointment_studio       ON appointment(studio_id);
CREATE INDEX idx_appointment_professional ON appointment(professional_id);
CREATE INDEX idx_appointment_client       ON appointment(client_id);
CREATE INDEX idx_appointment_status       ON appointment(status);
CREATE INDEX idx_appointment_start        ON appointment(start_datetime);
CREATE INDEX idx_appointment_token        ON appointment(token) WHERE token IS NOT NULL;

-- --------------------------------------------------------------------
-- 6. NUOVA TABELLA: service_type
--    Tipi di servizio offerti dai professionisti dello studio
-- --------------------------------------------------------------------
CREATE TABLE service_type (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id        UUID NOT NULL,
    professional_id  UUID,                 -- NULL = disponibile per tutti i professionisti
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    duration_minutes INT  NOT NULL,
    price            NUMERIC(10, 2),
    color            CHAR(7),              -- colore hex per calendario, es. #FF5733
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_service_type_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_service_type_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_service_duration
        CHECK (duration_minutes > 0),

    CONSTRAINT chk_service_price
        CHECK (price IS NULL OR price >= 0),

    CONSTRAINT chk_service_color
        CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX idx_service_type_studio ON service_type(studio_id);

-- Collega appointment a service_type
ALTER TABLE appointment
    ADD CONSTRAINT fk_appointment_service_type
        FOREIGN KEY (service_type_id)
        REFERENCES service_type(id)
        ON DELETE SET NULL;

-- --------------------------------------------------------------------
-- 7. NUOVA TABELLA: availability_exception
--    Giornate di chiusura o orari alternativi per un professionista
-- --------------------------------------------------------------------
CREATE TABLE availability_exception (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id  UUID NOT NULL,
    date             DATE NOT NULL,
    is_unavailable   BOOLEAN NOT NULL DEFAULT true,
    start_time       TIME,                 -- solo se is_unavailable = false
    end_time         TIME,                 -- solo se is_unavailable = false
    reason           VARCHAR(255),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_avail_exc_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_avail_exc_times
        CHECK (
            is_unavailable = true
            OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
        ),

    CONSTRAINT uq_avail_exc_professional_date
        UNIQUE (professional_id, date)
);

CREATE INDEX idx_avail_exc_professional_date
    ON availability_exception(professional_id, date);

-- --------------------------------------------------------------------
-- 8. NUOVA TABELLA: client_note
--    Storico note su un cliente, opzionalmente collegate a un appuntamento
-- --------------------------------------------------------------------
CREATE TABLE client_note (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      UUID NOT NULL,
    studio_id      UUID NOT NULL,
    author_id      UUID NOT NULL,          -- app_user che ha scritto la nota
    appointment_id UUID,                   -- opzionale: nota legata a un appuntamento
    content        TEXT NOT NULL,
    pinned         BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_client_note_client
        FOREIGN KEY (client_id)
        REFERENCES client(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_note_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_note_author
        FOREIGN KEY (author_id)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_client_note_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointment(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_client_note_client
    ON client_note(client_id, created_at DESC);

-- --------------------------------------------------------------------
-- 9. NUOVA TABELLA: notification
--    Storico e stato di ogni comunicazione inviata (email, WhatsApp, SMS)
-- --------------------------------------------------------------------
CREATE TABLE notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id       UUID NOT NULL,
    appointment_id  UUID,
    event_id        UUID,
    recipient_type  VARCHAR(50) NOT NULL,  -- CLIENT | PROFESSIONAL
    recipient_id    UUID NOT NULL,
    channel         VARCHAR(50) NOT NULL,  -- EMAIL | WHATSAPP | SMS
    type            VARCHAR(100) NOT NULL, -- es. APPOINTMENT_CONFIRMED, REMINDER_24H, ...
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    scheduled_at    TIMESTAMP NOT NULL,
    sent_at         TIMESTAMP,
    error_message   TEXT,
    payload         JSONB,                 -- corpo della notifica serializzato
    retry_count     SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_notification_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointment(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_notification_event
        FOREIGN KEY (event_id)
        REFERENCES event(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_notification_channel
        CHECK (channel IN ('EMAIL', 'WHATSAPP', 'SMS')),

    CONSTRAINT chk_notification_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DELIVERED', 'SKIPPED')),

    CONSTRAINT chk_notification_recipient_type
        CHECK (recipient_type IN ('CLIENT', 'PROFESSIONAL'))
);

-- Indice critico per il job di scheduling dei reminder
CREATE INDEX idx_notification_pending
    ON notification(scheduled_at, retry_count)
    WHERE status = 'PENDING';

CREATE INDEX idx_notification_appointment
    ON notification(appointment_id)
    WHERE appointment_id IS NOT NULL;
