package com.prenota24.backend.dto;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        String timestamp
) {
    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ErrorResponse(status, error, message, path, Instant.now().toString());
    }
}
