package com.prenota24.backend.controller;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.ClientSource;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppointmentService;
import com.prenota24.backend.service.IClientService;
import com.prenota24.backend.service.IProfessionalService;
import com.prenota24.backend.service.IServiceTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/book/{studioSlug}")
@RequiredArgsConstructor
public class PublicBookingController {

    private final StudioRepository studioRepository;
    private final IProfessionalService professionalService;
    private final IServiceTypeService serviceTypeService;
    private final IClientService clientService;
    private final IAppointmentService appointmentService;

    @GetMapping
    public StudioPublicResponse getStudio(@PathVariable String studioSlug) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var professionals = professionalService.getByStudio(studio.getId()).stream()
                .filter(ProfessionalResponse::active)
                .toList();

        return new StudioPublicResponse(
                studio.getName(),
                studio.getSlug(),
                studio.getTimezone(),
                professionals
        );
    }

    @GetMapping("/services")
    public List<ServiceTypeResponse> getServices(@PathVariable String studioSlug) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));
        return serviceTypeService.getByStudio(studio.getId());
    }

    @GetMapping("/professionals/{profId}/slots")
    public List<TimeSlotResponse> getSlots(@PathVariable String studioSlug,
                                            @PathVariable UUID profId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestParam(required = false) UUID serviceTypeId,
                                            @RequestParam(defaultValue = "60") int durationMinutes) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        // If serviceTypeId provided, use its duration
        if (serviceTypeId != null) {
            var service = serviceTypeService.getById(serviceTypeId, studio.getId());
            durationMinutes = service.durationMinutes();
        }

        return professionalService.getAvailableSlots(profId, date, durationMinutes, studio.getId());
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@PathVariable String studioSlug,
                                                  @RequestBody @Valid PublicBookingRequest request) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        // Find or create client
        var client = clientService.findOrCreateFromReservation(
                request.clientEmail(),
                request.clientFirstName() + " " + request.clientLastName(),
                request.clientPhone(),
                studio.getId()
        );

        // Create appointment as REQUESTED
        var appointmentRequest = new CreateAppointmentRequest(
                request.professionalId(),
                client.getId(),
                request.serviceTypeId(),
                request.startDatetime(),
                request.endDatetime(),
                request.notes(),
                false // Not confirmed immediately for public bookings
        );

        return appointmentService.create(appointmentRequest, studio.getId());
    }

    // ── Inner DTOs ──────────────────────────────────

    public record StudioPublicResponse(
            String name,
            String slug,
            String timezone,
            List<ProfessionalResponse> professionals
    ) {}

    public record PublicBookingRequest(
            @NotNull UUID professionalId,
            UUID serviceTypeId,
            @NotNull Instant startDatetime,
            @NotNull Instant endDatetime,
            @NotBlank String clientFirstName,
            @NotBlank String clientLastName,
            @NotBlank String clientEmail,
            String clientPhone,
            String notes
    ) {}
}
