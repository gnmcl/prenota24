package com.prenota24.backend.dto;

import java.util.List;

public record StudioPublicResponse(
        String name,
        String slug,
        String timezone,
        List<ProfessionalResponse> professionals
) {}
