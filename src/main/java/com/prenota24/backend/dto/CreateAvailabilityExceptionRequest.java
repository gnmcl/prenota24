package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateAvailabilityExceptionRequest(
        @NotNull LocalDate date,
        boolean isUnavailable,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}
