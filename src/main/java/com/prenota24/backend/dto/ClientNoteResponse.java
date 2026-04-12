package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientNoteResponse(
        UUID id,
        UUID clientId,
        UUID authorId,
        String authorName,
        UUID appointmentId,
        String content,
        boolean pinned,
        Instant createdAt
) {}
