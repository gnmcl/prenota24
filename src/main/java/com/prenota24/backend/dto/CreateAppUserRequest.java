package com.prenota24.backend.dto;

import com.prenota24.backend.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAppUserRequest(
        @NotNull UUID studioId,
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotNull UserRole role
        ) {
}
