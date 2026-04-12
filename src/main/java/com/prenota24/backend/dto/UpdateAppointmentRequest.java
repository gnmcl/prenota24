package com.prenota24.backend.dto;

import java.util.UUID;

public record UpdateAppointmentRequest(
        String notes,
        UUID serviceTypeId
) {}
