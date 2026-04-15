package com.prenota24.backend.controller;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppointmentService;
import com.prenota24.backend.service.IClientService;
import com.prenota24.backend.service.IProfessionalService;
import com.prenota24.backend.service.IServiceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Booking", description = "Flusso pubblico prenotazione appuntamenti (nessuna auth richiesta)")
@SecurityRequirements
public class PublicBookingController {

    private final StudioRepository studioRepository;
    private final IProfessionalService professionalService;
    private final IServiceTypeService serviceTypeService;
    private final IClientService clientService;
    private final IAppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Info studio pubblico", description = "Ritorna nome, slug, timezone e lista professionisti attivi")
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
    @Operation(summary = "Servizi disponibili dello studio")
    public List<ServiceTypeResponse> getServices(@PathVariable String studioSlug) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));
        return serviceTypeService.getByStudio(studio.getId());
    }

    @GetMapping("/professionals/{profId}/slots")
    @Operation(summary = "Slot disponibili per un professionista", description = "Restituisce slot liberi per la data richiesta")
    public List<TimeSlotResponse> getSlots(@PathVariable String studioSlug,
                                            @PathVariable UUID profId,
                                            @Parameter(description = "Data nel formato ISO (es. 2026-05-01)")
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestParam(required = false) UUID serviceTypeId,
                                            @RequestParam(defaultValue = "60") int durationMinutes) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        if (serviceTypeId != null) {
            var service = serviceTypeService.getById(serviceTypeId, studio.getId());
            durationMinutes = service.durationMinutes();
        }

        return professionalService.getAvailableSlots(profId, date, durationMinutes, studio.getId());
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prenota appuntamento pubblico", description = "Crea/trova il cliente e crea appuntamento in stato REQUESTED")
    @ApiResponse(responseCode = "201", description = "Appuntamento creato")
    @ApiResponse(responseCode = "409", description = "Slot già occupato")
    public AppointmentResponse createAppointment(@PathVariable String studioSlug,
                                                  @RequestBody @Valid PublicBookingRequest request) {
        var studio = studioRepository.findBySlug(studioSlug)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var client = clientService.findOrCreateFromReservation(
                request.clientEmail(),
                request.clientFirstName() + " " + request.clientLastName(),
                request.clientPhone(),
                studio.getId()
        );

        var appointmentRequest = new CreateAppointmentRequest(
                request.professionalId(),
                client.getId(),
                request.serviceTypeId(),
                request.startDatetime(),
                request.endDatetime(),
                request.notes(),
                false
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