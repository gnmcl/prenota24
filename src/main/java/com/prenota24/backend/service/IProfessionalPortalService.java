package com.prenota24.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.AvailabilityExceptionResponse;
import com.prenota24.backend.dto.AvailabilityResponse;
import com.prenota24.backend.dto.AvailabilitySlotRequest;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.dto.ClientSummaryResponse;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.CreateAvailabilityExceptionRequest;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.dto.ProfessionalDashboardResponse;
import com.prenota24.backend.dto.ProposeNewTimeRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.TimeSlotResponse;

public interface IProfessionalPortalService {

    ProfessionalDashboardResponse getDashboard(UUID professionalId, UUID studioId);

    Page<AppointmentResponse> getMyAppointments(UUID professionalId, String status, Pageable pageable);

    AppointmentResponse getMyAppointmentById(UUID appointmentId, UUID professionalId);

    List<ClientSummaryResponse> getMyClients(UUID professionalId);

    ClientSummaryResponse createClient(CreateClientRequest request, UUID professionalId, UUID studioId);

    List<ServiceTypeResponse> getMyServiceTypes(UUID professionalId, UUID studioId);

    AppointmentResponse createAppointment(CreateAppointmentRequest request, UUID professionalId, UUID studioId);

    AppointmentResponse confirmAppointment(UUID appointmentId, UUID professionalId);

    AppointmentResponse cancelAppointment(UUID appointmentId, CancelAppointmentRequest request, UUID professionalId);

    AppointmentResponse completeAppointment(UUID appointmentId, UUID professionalId);

    AppointmentResponse noShowAppointment(UUID appointmentId, UUID professionalId);

    AppointmentResponse proposeNewTime(UUID appointmentId, ProposeNewTimeRequest request, UUID professionalId, UUID studioId);

    List<TimeSlotResponse> getMyAvailableSlots(UUID professionalId, LocalDate date, int durationMinutes, UUID studioId);

    // ── Availability ──────────────────────────────────────

    List<AvailabilityResponse> getMyAvailability(UUID professionalId, UUID studioId);

    List<AvailabilityResponse> setMyAvailability(UUID professionalId, List<AvailabilitySlotRequest> slots, UUID studioId);

    List<AvailabilityExceptionResponse> getMyExceptions(UUID professionalId, UUID studioId);

    AvailabilityExceptionResponse addMyException(UUID professionalId, CreateAvailabilityExceptionRequest request, UUID studioId);

    void removeMyException(UUID professionalId, UUID exceptionId, UUID studioId);
}
