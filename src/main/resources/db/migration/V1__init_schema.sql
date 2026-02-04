CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE studio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    timezone VARCHAR(50) DEFAULT 'Europe/Rome',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_email UNIQUE (email)
);

CREATE TABLE professional (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_professional_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE
);

CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_client_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE
);

CREATE TABLE availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professional_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_availability_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_availability_time
        CHECK (start_time < end_time)
);

CREATE TABLE booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    client_id UUID NOT NULL,
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_booking_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_client
        FOREIGN KEY (client_id)
        REFERENCES client(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_booking_time
        CHECK (start_datetime < end_datetime)
);

ALTER TABLE booking
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist (
    professional_id WITH =,
    tsrange(start_datetime, end_datetime) WITH &&
)
WHERE (status = 'CONFIRMED');
