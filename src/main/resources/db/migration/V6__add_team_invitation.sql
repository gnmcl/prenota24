-- ──────────────────────────────────────────────────────────────────────────────
-- V6: Add team_invitation table for professional invitation flow
-- ──────────────────────────────────────────────────────────────────────────────

CREATE TABLE team_invitation (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    studio_id      UUID         NOT NULL,
    professional_id UUID        NOT NULL,
    email          VARCHAR(255) NOT NULL,
    token          VARCHAR(64)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at     TIMESTAMPTZ  NOT NULL,
    accepted_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_team_invitation PRIMARY KEY (id),
    CONSTRAINT uq_team_invitation_token UNIQUE (token),

    CONSTRAINT fk_ti_studio
        FOREIGN KEY (studio_id)
        REFERENCES studio(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ti_professional
        FOREIGN KEY (professional_id)
        REFERENCES professional(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ti_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_ti_studio_id      ON team_invitation(studio_id);
CREATE INDEX idx_ti_professional_id ON team_invitation(professional_id);
CREATE INDEX idx_ti_token          ON team_invitation(token);
