package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfessionalResponse(
        UUID id,
        UUID studioId,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean active,
        Instant createdAt
) {}
