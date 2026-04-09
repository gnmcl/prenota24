package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String guestName,
        String guestEmail,
        String guestPhone,
        String notes,
        String status,
        Instant createdAt
) {
}
