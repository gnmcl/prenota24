-- ============================================================
-- V2: Add Event and Reservation tables for Prenota24
-- ============================================================

-- Add name column to app_user for simplified registration
ALTER TABLE app_user ADD COLUMN name VARCHAR(200);

CREATE TABLE event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    created_by UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(100) NOT NULL,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(255),
    max_participants INT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_event_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_event_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_user(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_event_slug UNIQUE (slug),

    CONSTRAINT chk_event_time
        CHECK (start_time < end_time),

    CONSTRAINT chk_event_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_event_studio_id ON event(studio_id);
CREATE INDEX idx_event_slug ON event(slug);
CREATE INDEX idx_event_created_by ON event(created_by);

-- ============================================================

CREATE TABLE reservation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    guest_name VARCHAR(200) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    guest_phone VARCHAR(50),
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_reservation_event
        FOREIGN KEY (event_id)
        REFERENCES event(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_reservation_event_email
        UNIQUE (event_id, guest_email),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_reservation_event_id ON reservation(event_id);
