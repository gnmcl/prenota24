package com.prenota24.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServiceTypeResponse(
        UUID id,
        UUID studioId,
        List<UUID> professionalIds,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price,
        String color,
        boolean active,
        Instant createdAt
) {}
