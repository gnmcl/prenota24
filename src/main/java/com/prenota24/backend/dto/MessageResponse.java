package com.prenota24.backend.dto;

import com.prenota24.backend.domain.MessageDirection;
import com.prenota24.backend.domain.MessageKind;
import com.prenota24.backend.domain.MessageStatus;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        long sequenceNo,
        UUID conversationId,
        String text,
        MessageDirection direction,
        MessageKind kind,
        MessageStatus status,
        Instant createdAt,
        UUID appointmentId,
        String senderName,
        String errorMessage
) {}
