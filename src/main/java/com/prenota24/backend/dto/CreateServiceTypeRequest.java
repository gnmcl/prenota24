package com.prenota24.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateServiceTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive Integer durationMinutes,
        BigDecimal price,
        String color,
        List<UUID> professionalIds
) {}
