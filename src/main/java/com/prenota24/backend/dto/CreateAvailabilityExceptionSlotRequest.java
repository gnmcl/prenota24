package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateAvailabilityExceptionSlotRequest(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
