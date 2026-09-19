package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicBookingRequest(
        @NotNull UUID professionalId,
        UUID serviceTypeId,
        @NotNull Instant startDatetime,
        @NotNull Instant endDatetime,
        @NotBlank @Size(max = 100) String clientFirstName,
        @NotBlank @Size(max = 100) String clientLastName,
        @NotBlank @Email @Size(max = 255) String clientEmail,
        @Size(max = 50) String clientPhone,
        String notes
) {}
