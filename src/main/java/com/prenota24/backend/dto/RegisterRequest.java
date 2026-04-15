package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Size(min = 1, max = 255) String studioName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password
) {
}
