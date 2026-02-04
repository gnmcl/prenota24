package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStudioRequest(
        @NotBlank String name,
        String email,
        String phone
) {
}
