package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateAppointmentRequest(
        String notes,
        UUID serviceTypeId,
        Instant startDatetime,
        Instant endDatetime,
        UUID professionalId
) {}
