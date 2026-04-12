package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProfessionalRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        String phone
) {}
