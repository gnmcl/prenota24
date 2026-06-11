package com.prenota24.backend.dto;

import java.util.Map;

/**
 * Strongly-typed representation of an outbound email notification.
 * Converts to a Map for JSON persistence in the Notification entity.
 */
public record EmailPayload(
        String recipientEmail,
        String recipientName,
        String subject,
        String body
) {
    public Map<String, Object> toMap() {
        return Map.of(
                "recipientEmail", recipientEmail,
                "recipientName", recipientName,
                "subject", subject,
                "body", body
        );
    }
}

