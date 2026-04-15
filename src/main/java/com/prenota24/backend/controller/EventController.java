package com.prenota24.backend.controller;

import com.prenota24.backend.domain.EventStatus;
import com.prenota24.backend.dto.CreateEventRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.EventSummaryResponse;
import com.prenota24.backend.dto.ReservationResponse;
import com.prenota24.backend.service.IEventService;
import com.prenota24.backend.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Gestione eventi dello studio (richiede auth)")
public class EventController {

    private final IEventService eventService;
    private final IReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea evento", description = "L'evento viene creato in stato DRAFT")
    public EventResponse create(@RequestBody @Valid CreateEventRequest request,
                                Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.create(request, userId);
    }

    @GetMapping
    @Operation(summary = "Lista eventi dell'utente corrente")
    public List<EventSummaryResponse> getMyEvents(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.getByUserId(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio evento")
    public EventResponse getById(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.getById(id, userId);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambia stato evento", description = "Body: `{\"status\": \"PUBLISHED\"}`. Stati validi: DRAFT, PUBLISHED, CANCELLED, COMPLETED")
    public EventResponse updateStatus(@PathVariable UUID id,
                                      @RequestBody Map<String, String> body,
                                      Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        EventStatus status = EventStatus.valueOf(body.get("status"));
        return eventService.updateStatus(id, status, userId);
    }

    @GetMapping("/{id}/reservations")
    @Operation(summary = "Lista prenotazioni per un evento")
    public List<ReservationResponse> getReservations(@PathVariable UUID id) {
        return reservationService.getByEventId(id);
    }

    @PatchMapping("/reservations/{reservationId}/cancel")
    @Operation(summary = "Cancella prenotazione evento")
    public ReservationResponse cancelReservation(@PathVariable UUID reservationId) {
        return reservationService.cancel(reservationId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina evento e tutte le sue prenotazioni")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        eventService.delete(id, userId);
    }
}