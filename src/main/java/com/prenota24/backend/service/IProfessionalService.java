package com.prenota24.backend.service;

import com.prenota24.backend.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IProfessionalService {

    ProfessionalResponse create(CreateProfessionalRequest request, UUID studioId);

    ProfessionalResponse getById(UUID id, UUID studioId);

    List<ProfessionalResponse> getByStudio(UUID studioId);

    ProfessionalResponse update(UUID id, UpdateProfessionalRequest request, UUID studioId);

    void delete(UUID id, UUID studioId);

    List<AvailabilityResponse> getAvailability(UUID professionalId, UUID studioId);

    List<AvailabilityResponse> setAvailability(UUID professionalId, List<AvailabilitySlotRequest> slots, UUID studioId);

    List<AvailabilityExceptionResponse> getExceptions(UUID professionalId, UUID studioId);

    AvailabilityExceptionResponse addException(UUID professionalId, CreateAvailabilityExceptionRequest request, UUID studioId);

    void removeException(UUID professionalId, UUID exceptionId, UUID studioId);

    List<TimeSlotResponse> getAvailableSlots(UUID professionalId, LocalDate date, int durationMinutes, UUID studioId);
}
