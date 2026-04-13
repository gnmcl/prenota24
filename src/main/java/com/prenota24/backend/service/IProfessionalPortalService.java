package com.prenota24.backend.service;

import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IProfessionalPortalService {

    ProfessionalDashboardResponse getDashboard(UUID professionalId, UUID studioId);

    Page<AppointmentResponse> getMyAppointments(UUID professionalId, String status, Pageable pageable);

    AppointmentResponse getMyAppointmentById(UUID appointmentId, UUID professionalId);

    List<ClientSummaryResponse> getMyClients(UUID professionalId);

    AppointmentResponse createAppointment(CreateAppointmentRequest request, UUID professionalId, UUID studioId);

    AppointmentResponse confirmAppointment(UUID appointmentId, UUID professionalId);

    AppointmentResponse cancelAppointment(UUID appointmentId, CancelAppointmentRequest request, UUID professionalId);

    AppointmentResponse completeAppointment(UUID appointmentId, UUID professionalId);

    AppointmentResponse noShowAppointment(UUID appointmentId, UUID professionalId);

    // ── Availability ──────────────────────────────────────

    List<AvailabilityResponse> getMyAvailability(UUID professionalId, UUID studioId);

    List<AvailabilityResponse> setMyAvailability(UUID professionalId, List<AvailabilitySlotRequest> slots, UUID studioId);

    List<AvailabilityExceptionResponse> getMyExceptions(UUID professionalId, UUID studioId);

    AvailabilityExceptionResponse addMyException(UUID professionalId, CreateAvailabilityExceptionRequest request, UUID studioId);

    void removeMyException(UUID professionalId, UUID exceptionId, UUID studioId);
}
