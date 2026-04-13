package com.prenota24.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull UUID professionalId,
        @Email @NotBlank String email
) {}
