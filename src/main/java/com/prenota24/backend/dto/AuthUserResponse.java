package com.prenota24.backend.dto;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String name,
        String role,
        UUID studioId,
        UUID professionalId
) {
}
