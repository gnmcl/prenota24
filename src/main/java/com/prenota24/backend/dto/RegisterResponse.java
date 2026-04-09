package com.prenota24.backend.dto;

public record RegisterResponse(
        String accessToken,
        AuthUserResponse user
) {
}
