package com.prenota24.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AvailabilitySlotRequest(
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
