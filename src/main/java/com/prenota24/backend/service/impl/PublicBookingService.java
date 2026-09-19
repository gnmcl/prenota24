package com.prenota24.backend.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.SlotNotAvailableException;
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.ProfessionalResponse;
import com.prenota24.backend.dto.PublicBookingRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.StudioPublicResponse;
import com.prenota24.backend.dto.TimeSlotResponse;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.repository.ServiceTypeRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppointmentService;
import com.prenota24.backend.service.IClientService;
import com.prenota24.backend.service.IProfessionalService;
import com.prenota24.backend.service.IPublicBookingService;
import com.prenota24.backend.service.IServiceTypeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicBookingService implements IPublicBookingService {

    private final StudioRepository studioRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final IProfessionalService professionalService;
    private final IServiceTypeService serviceTypeService;
    private final IClientService clientService;
    private final IAppointmentService appointmentService;
    private final ServiceTypeAssignmentValidator assignmentValidator;

    @Override
    @Transactional(readOnly = true)
    public StudioPublicResponse getStudio(String studioSlug) {
        var studio = findStudio(studioSlug);
        var professionals = professionalService.getByStudio(studio.getId()).stream()
                .filter(ProfessionalResponse::active)
                .toList();
        return new StudioPublicResponse(
                studio.getName(), studio.getSlug(), timezoneOf(studio), professionals);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> getServices(String studioSlug) {
        return serviceTypeService.getByStudio(findStudio(studioSlug).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(
            String studioSlug,
            UUID professionalId,
            LocalDate date,
            UUID serviceTypeId,
            int durationMinutes) {
        var studio = findStudio(studioSlug);
        var zone = ZoneId.of(timezoneOf(studio));
        if (date.isBefore(LocalDate.now(zone))) {
            throw new IllegalArgumentException("Non è possibile prenotare una data passata");
        }
        validateProfessional(professionalId, studio.getId());

        if (serviceTypeId != null) {
            var serviceType = serviceTypeRepository.findByIdAndStudioId(serviceTypeId, studio.getId())
                    .filter(service -> service.isActive())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
            assignmentValidator.validate(serviceType, professionalId);
            durationMinutes = serviceType.getDurationMinutes();
        }

        return professionalService.getAvailableSlots(professionalId, date, durationMinutes, studio.getId());
    }

    @Override
    @Transactional
    public AppointmentResponse createAppointment(String studioSlug, PublicBookingRequest request) {
        var studio = findStudio(studioSlug);
        if (!request.startDatetime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Non è possibile prenotare un orario passato");
        }
        validateProfessional(request.professionalId(), studio.getId());
        var duration = Duration.between(request.startDatetime(), request.endDatetime());
        var durationMinutes = duration.toMinutes();
        if (duration.isZero() || duration.isNegative()
                || !duration.minusMinutes(durationMinutes).isZero()
                || durationMinutes > 1440) {
            throw new IllegalArgumentException("La durata dell'appuntamento non è valida");
        }
        var durationMinutesValue = Math.toIntExact(durationMinutes);
        if (request.serviceTypeId() != null) {
            var serviceType = serviceTypeRepository.findByIdAndStudioId(request.serviceTypeId(), studio.getId())
                    .filter(service -> service.isActive())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo di servizio non trovato"));
            assignmentValidator.validate(serviceType, request.professionalId());
            if (durationMinutesValue != serviceType.getDurationMinutes()) {
                throw new IllegalArgumentException("La durata non corrisponde al servizio selezionato");
            }
        }
        validateAvailableSlot(studio, request, durationMinutesValue);

        var client = clientService.findOrCreateFromReservation(
                request.clientEmail(),
                request.clientFirstName() + " " + request.clientLastName(),
                request.clientPhone(),
                studio.getId());

        var appointmentRequest = new CreateAppointmentRequest(
                request.professionalId(),
                client.getId(),
                request.serviceTypeId(),
                request.startDatetime(),
                request.endDatetime(),
                request.notes(),
                false);
        return appointmentService.create(appointmentRequest, studio.getId());
    }

    private Studio findStudio(String studioSlug) {
        return studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));
    }

    private void validateProfessional(UUID professionalId, UUID studioId) {
        var professional = professionalService.getById(professionalId, studioId);
        if (!professional.active()) {
            throw new EntityNotFoundException("Professionista non trovato");
        }
    }

    private void validateAvailableSlot(Studio studio, PublicBookingRequest request, int durationMinutes) {
        var date = request.startDatetime().atZone(ZoneId.of(timezoneOf(studio))).toLocalDate();
        var available = professionalService.getAvailableSlots(
                        request.professionalId(), date, durationMinutes, studio.getId())
                .stream()
                .anyMatch(slot -> slot.start().equals(request.startDatetime())
                        && slot.end().equals(request.endDatetime()));
        if (!available) {
            throw new SlotNotAvailableException("Lo slot selezionato non è disponibile");
        }
    }

    private String timezoneOf(Studio studio) {
        return studio.getTimezone() != null ? studio.getTimezone() : "Europe/Rome";
    }
}
