package com.prenota24.backend.dto;

import com.prenota24.backend.domain.ClientSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        UUID studioId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String notes,
        ClientSource source,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {}
