package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant createdAt
) {}
