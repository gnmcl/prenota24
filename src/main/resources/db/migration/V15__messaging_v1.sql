CREATE TABLE conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    client_id UUID NOT NULL,
    whatsapp_opt_in BOOLEAN NOT NULL DEFAULT false,
    whatsapp_opt_in_phone VARCHAR(50),
    last_inbound_at TIMESTAMPTZ,
    last_inbound_phone VARCHAR(50),
    last_message_at TIMESTAMPTZ,
    last_message_sequence_no BIGINT,
    last_message_preview VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_conversation_studio FOREIGN KEY (studio_id) REFERENCES studio(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE,
    CONSTRAINT uq_conversation_studio_client UNIQUE (studio_id, client_id)
);

CREATE INDEX idx_conversation_studio_activity
    ON conversation(studio_id, last_message_at DESC NULLS LAST, created_at DESC);

CREATE TABLE conversation_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sequence_no BIGSERIAL NOT NULL UNIQUE,
    conversation_id UUID NOT NULL,
    appointment_id UUID,
    sender_user_id UUID,
    request_id UUID,
    request_fingerprint VARCHAR(64),
    provider_message_id VARCHAR(200),
    recipient_phone VARCHAR(50),
    text TEXT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sender_name VARCHAR(200),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_message_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(id) ON DELETE SET NULL,
    CONSTRAINT fk_conversation_message_sender_user FOREIGN KEY (sender_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_conversation_message_direction CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT chk_conversation_message_kind CHECK (kind IN ('TEXT', 'TEMPLATE', 'UNSUPPORTED')),
    CONSTRAINT chk_conversation_message_status CHECK (status IN ('RECEIVED', 'QUEUED', 'SENDING', 'ACCEPTED', 'DELIVERED', 'READ', 'FAILED', 'UNKNOWN')),
    CONSTRAINT uq_conversation_message_request UNIQUE (conversation_id, request_id)
);

CREATE UNIQUE INDEX uq_conversation_message_provider
    ON conversation_message(provider_message_id) WHERE provider_message_id IS NOT NULL;
CREATE INDEX idx_conversation_message_history
    ON conversation_message(conversation_id, sequence_no DESC);
CREATE INDEX idx_conversation_message_dispatch
    ON conversation_message(status, created_at) WHERE status = 'QUEUED';

CREATE TABLE conversation_read_cursor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    last_read_message_id UUID NOT NULL,
    last_read_message_created_at TIMESTAMPTZ NOT NULL,
    last_read_sequence_no BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_read_cursor_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_cursor_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_cursor_message FOREIGN KEY (last_read_message_id) REFERENCES conversation_message(id) ON DELETE CASCADE,
    CONSTRAINT uq_read_cursor_conversation_user UNIQUE (conversation_id, user_id)
);

CREATE TABLE conversation_consent_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL,
    recipient_phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_consent_audit_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_audit_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_consent_audit_conversation
    ON conversation_consent_audit(conversation_id, created_at DESC);

CREATE TABLE whatsapp_inbound_quarantine (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_message_id VARCHAR(200) NOT NULL,
    destination_phone_number_id VARCHAR(100),
    sender_phone VARCHAR(50),
    text TEXT,
    reason VARCHAR(50) NOT NULL,
    raw_payload TEXT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_whatsapp_quarantine_provider_message UNIQUE (provider_message_id)
);
