package com.prenota24.backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.CancelledBy;

public record AppointmentResponse(
        UUID id,
        UUID studioId,
        UUID professionalId,
        String professionalFullName,
        UUID clientId,
        String clientFullName,
        UUID serviceTypeId,
        String serviceTypeName,
        String serviceTypeColor,
        Instant startDatetime,
        Instant endDatetime,
        AppointmentStatus status,
        String notes,
        Instant proposedStart,
        Instant proposedEnd,
        String cancellationReason,
        CancelledBy cancelledBy,
        String token,
        Instant createdAt,
        Instant updatedAt
) {}
