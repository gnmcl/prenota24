package com.prenota24.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceTypeResponse(
        UUID id,
        UUID studioId,
        UUID professionalId,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        String color,
        boolean active,
        Instant createdAt
) {}
