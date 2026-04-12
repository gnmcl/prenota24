package com.prenota24.backend.dto;

import java.util.List;

public record UpdateClientRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String notes,
        List<String> tags
) {}
