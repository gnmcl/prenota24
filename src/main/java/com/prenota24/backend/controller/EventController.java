package com.prenota24.backend.controller;

import com.prenota24.backend.domain.EventStatus;
import com.prenota24.backend.dto.CreateEventRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.EventSummaryResponse;
import com.prenota24.backend.dto.ReservationResponse;
import com.prenota24.backend.service.IEventService;
import com.prenota24.backend.service.IReservationService;
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
public class EventController {

    private final IEventService eventService;
    private final IReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestBody @Valid CreateEventRequest request,
                                Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.create(request, userId);
    }

    @GetMapping
    public List<EventSummaryResponse> getMyEvents(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.getByUserId(userId);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return eventService.getById(id, userId);
    }

    @PatchMapping("/{id}/status")
    public EventResponse updateStatus(@PathVariable UUID id,
                                      @RequestBody Map<String, String> body,
                                      Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        EventStatus status = EventStatus.valueOf(body.get("status"));
        return eventService.updateStatus(id, status, userId);
    }

    @GetMapping("/{id}/reservations")
    public List<ReservationResponse> getReservations(@PathVariable UUID id) {
        return reservationService.getByEventId(id);
    }

    @PatchMapping("/reservations/{reservationId}/cancel")
    public ReservationResponse cancelReservation(@PathVariable UUID reservationId) {
        return reservationService.cancel(reservationId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        eventService.delete(id, userId);
    }
}
