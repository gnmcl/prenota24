package com.prenota24.backend.dto;

import com.prenota24.backend.domain.UserRole;

import java.util.UUID;

public record AppUserResponse(
        UUID id,
        UUID studioId,
        String email,
        UserRole role,
        Boolean isActive) {
}
