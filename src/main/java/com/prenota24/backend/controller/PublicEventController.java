package com.prenota24.backend.controller;

import com.prenota24.backend.dto.CreateReservationRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.ReservationResponse;
import com.prenota24.backend.service.IEventService;
import com.prenota24.backend.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/events")
@RequiredArgsConstructor
@Tag(name = "Public Events", description = "Visualizzazione eventi e prenotazione pubblica (nessuna auth)")
@SecurityRequirements
public class PublicEventController {

    private final IEventService eventService;
    private final IReservationService reservationService;

    @GetMapping("/{slug}")
    @Operation(summary = "Dettaglio evento pubblico")
    public EventResponse getBySlug(@PathVariable String slug) {
        return eventService.getBySlug(slug);
    }

    @PostMapping("/{slug}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prenota posto a evento", description = "Vincolo: una sola prenotazione per email per evento")
    @ApiResponse(responseCode = "201", description = "Prenotazione confermata")
    @ApiResponse(responseCode = "400", description = "Evento non pubblicato, email duplicata o posti esauriti")
    public ReservationResponse createReservation(@PathVariable String slug,
                                                  @RequestBody @Valid CreateReservationRequest request) {
        return reservationService.create(slug, request);
    }
}
