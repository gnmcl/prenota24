package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateClientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String email,
        String phone,
        String notes,
        List<String> tags
) {}
