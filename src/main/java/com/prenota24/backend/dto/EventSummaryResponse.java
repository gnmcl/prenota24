package com.prenota24.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String title,
        String slug,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Integer maxParticipants,
        long currentParticipants,
        String status
) {
}
