package com.prenota24.backend.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateClientNoteRequest(
        @NotBlank String content,
        UUID appointmentId,
        Boolean pinned
) {}
