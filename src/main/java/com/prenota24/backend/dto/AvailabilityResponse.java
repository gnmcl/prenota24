package com.prenota24.backend.dto;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}
