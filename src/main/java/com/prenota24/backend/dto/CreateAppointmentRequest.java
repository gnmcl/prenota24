package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID professionalId,
        @NotNull UUID clientId,
        UUID serviceTypeId,
        @NotNull Instant startDatetime,
        @NotNull Instant endDatetime,
        String notes,
        boolean confirmImmediately
) {}
