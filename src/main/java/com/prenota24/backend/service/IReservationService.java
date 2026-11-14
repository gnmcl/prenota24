package com.prenota24.backend.service;

import com.prenota24.backend.dto.CreateReservationRequest;
import com.prenota24.backend.dto.ReservationResponse;

import java.util.List;
import java.util.UUID;

public interface IReservationService {

    ReservationResponse create(String slug, CreateReservationRequest request);

    /** Prenotazioni di un evento; solo per il creatore dell'evento, altrimenti 404. */
    List<ReservationResponse> getByEventId(UUID eventId, UUID userId);

    /** Cancella una prenotazione; solo per il creatore dell'evento, altrimenti 404. */
    ReservationResponse cancel(UUID reservationId, UUID userId);
}
