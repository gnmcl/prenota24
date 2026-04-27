package com.prenota24.backend.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilityExceptionResponse(
        UUID id,
        LocalDate date,
        List<AvailabilityExceptionSlotResponse> slots,
        boolean isUnavailableAllDay,
        String reason
) {}
