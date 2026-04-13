package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID professionalId,
        String professionalName,
        String email,
        String status,
        String inviteLink,
        Instant expiresAt,
        Instant createdAt
) {}
