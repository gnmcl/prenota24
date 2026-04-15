package com.prenota24.backend.dto;

import jakarta.validation.constraints.Size;

public record EditStudioProfileRequest(
        @Size(min = 1, max = 255) String name,
        String email,
        String phone,
        String timezone
) {}
