package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive Integer durationMinutes,
        BigDecimal price,
        String color,
        UUID professionalId
) {}
