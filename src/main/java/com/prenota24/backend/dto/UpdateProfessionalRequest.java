package com.prenota24.backend.dto;

public record UpdateProfessionalRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        Boolean active
) {}
