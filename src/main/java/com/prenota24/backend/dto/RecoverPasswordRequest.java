package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverPasswordRequest(
        @Email @NotBlank String email
) {}

