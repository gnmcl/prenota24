package com.prenota24.backend.dto;

public record InvitationInfoResponse(
        String professionalName,
        String studioName,
        String email,
        String status
) {}
