package com.prenota24.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String slug,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Integer maxParticipants,
        long currentParticipants,
        String status,
        String shareLink,
        Instant createdAt,
        Instant updatedAt
) {
}
