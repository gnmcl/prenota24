package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 6) String code,
        @NotBlank @Size(min = 8, message = "La nuova password deve contenere almeno 8 caratteri") String newPassword
) {}

