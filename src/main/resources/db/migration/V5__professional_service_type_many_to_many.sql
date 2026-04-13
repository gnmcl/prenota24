-- ====================================================================
-- V5: Professional ↔ ServiceType many-to-many
-- ====================================================================

CREATE TABLE professional_service_type (
    professional_id UUID NOT NULL,
    service_type_id UUID NOT NULL,

    PRIMARY KEY (professional_id, service_type_id),

    CONSTRAINT fk_pst_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_pst_service_type
        FOREIGN KEY (service_type_id)
        REFERENCES service_type(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_pst_service_type ON professional_service_type(service_type_id);

-- Migrate existing single-FK data
INSERT INTO professional_service_type (professional_id, service_type_id)
SELECT professional_id, id
FROM service_type
WHERE professional_id IS NOT NULL;

-- Drop the old single-FK column
ALTER TABLE service_type DROP COLUMN professional_id;
