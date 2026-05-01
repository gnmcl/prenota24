package com.prenota24.backend.dto;

import com.prenota24.backend.domain.AppointmentCapacityLevel;

import java.time.LocalDate;

public record DayAppointmentCountResponse(
        LocalDate date,
        long count,
        AppointmentCapacityLevel capacityLevel
) {}
