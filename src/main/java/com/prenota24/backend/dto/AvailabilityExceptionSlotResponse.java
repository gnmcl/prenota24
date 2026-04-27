package com.prenota24.backend.dto;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityExceptionSlotResponse(
        UUID id,
        LocalTime startTime,
        LocalTime endTime
) {
}
