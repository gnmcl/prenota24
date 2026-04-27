package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateAvailabilityExceptionRequest(
        @NotNull LocalDate date,
        boolean isUnavailableAllDay,
        @NotNull List<CreateAvailabilityExceptionSlotRequest> slots,
        String reason
) {}
