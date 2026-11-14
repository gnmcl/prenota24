package com.prenota24.backend.dto;

import com.prenota24.backend.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param studioId ignorato dal backend: lo studio è sempre quello dell'admin autenticato.
 *                 Mantenuto opzionale solo per compatibilità con i client che lo inviano ancora.
 */
public record CreateAppUserRequest(
        @Deprecated UUID studioId,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull UserRole role
        ) {
}
