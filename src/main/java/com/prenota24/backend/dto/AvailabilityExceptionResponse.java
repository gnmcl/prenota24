package com.prenota24.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityExceptionResponse(
        UUID id,
        LocalDate date,
        boolean isUnavailable,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}
