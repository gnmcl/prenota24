package com.prenota24.backend.controller;

import com.prenota24.backend.dto.CreateReservationRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.ReservationResponse;
import com.prenota24.backend.service.IEventService;
import com.prenota24.backend.service.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final IEventService eventService;
    private final IReservationService reservationService;

    @GetMapping("/{slug}")
    public EventResponse getBySlug(@PathVariable String slug) {
        return eventService.getBySlug(slug);
    }

    @PostMapping("/{slug}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(@PathVariable String slug,
                                                  @RequestBody @Valid CreateReservationRequest request) {
        return reservationService.create(slug, request);
    }
}
