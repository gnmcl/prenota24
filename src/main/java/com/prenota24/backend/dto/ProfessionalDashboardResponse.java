package com.prenota24.backend.dto;

public record ProfessionalDashboardResponse(
        ProfessionalResponse professional,
        StudioResponse studio,
        long todayAppointments,
        long totalClients,
        long pendingAppointments
) {}
