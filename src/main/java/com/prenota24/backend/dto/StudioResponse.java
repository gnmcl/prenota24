package com.prenota24.backend.dto;

import java.util.UUID;

public record StudioResponse(
        UUID id,
        String name,
        String slug,
        String email,
        String phone,
        String timezone,
        Integer maxAppointmentsPerDay,
        Integer warningThreshold,
        Integer criticalThreshold
) {
}
