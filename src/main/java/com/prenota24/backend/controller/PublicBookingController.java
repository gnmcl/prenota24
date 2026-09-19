package com.prenota24.backend.controller;

import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.PublicBookingRequest;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.StudioPublicResponse;
import com.prenota24.backend.dto.TimeSlotResponse;
import com.prenota24.backend.service.IPublicBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/book/{studioSlug}")
@RequiredArgsConstructor
@Tag(name = "Public Booking", description = "Flusso pubblico prenotazione appuntamenti (nessuna auth richiesta)")
@SecurityRequirements
public class PublicBookingController {

    private final IPublicBookingService publicBookingService;

    @GetMapping
    @Operation(summary = "Info studio pubblico", description = "Ritorna nome, slug, timezone e lista professionisti attivi")
    public StudioPublicResponse getStudio(@PathVariable String studioSlug) {
        return publicBookingService.getStudio(studioSlug);
    }

    @GetMapping("/services")
    @Operation(summary = "Servizi disponibili dello studio")
    public List<ServiceTypeResponse> getServices(@PathVariable String studioSlug) {
        return publicBookingService.getServices(studioSlug);
    }

    @GetMapping("/professionals/{profId}/slots")
    @Operation(summary = "Slot disponibili per un professionista", description = "Restituisce slot liberi per la data richiesta")
    public List<TimeSlotResponse> getSlots(@PathVariable String studioSlug,
                                            @PathVariable UUID profId,
                                            @Parameter(description = "Data nel formato ISO (es. 2026-05-01)")
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                            @RequestParam(required = false) UUID serviceTypeId,
                                            @RequestParam(defaultValue = "60") int durationMinutes) {
        return publicBookingService.getAvailableSlots(
                studioSlug, profId, date, serviceTypeId, durationMinutes);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prenota appuntamento pubblico", description = "Crea/trova il cliente e crea appuntamento in stato REQUESTED")
    @ApiResponse(responseCode = "201", description = "Appuntamento creato")
    @ApiResponse(responseCode = "409", description = "Slot già occupato")
    public AppointmentResponse createAppointment(@PathVariable String studioSlug,
                                                  @RequestBody @Valid PublicBookingRequest request) {
        return publicBookingService.createAppointment(studioSlug, request);
    }
}
