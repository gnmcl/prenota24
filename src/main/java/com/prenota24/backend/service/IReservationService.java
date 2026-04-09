package com.prenota24.backend.service;

import com.prenota24.backend.dto.CreateReservationRequest;
import com.prenota24.backend.dto.ReservationResponse;

import java.util.List;
import java.util.UUID;

public interface IReservationService {

    ReservationResponse create(String slug, CreateReservationRequest request);

    List<ReservationResponse> getByEventId(UUID eventId);

    ReservationResponse cancel(UUID reservationId);
}
