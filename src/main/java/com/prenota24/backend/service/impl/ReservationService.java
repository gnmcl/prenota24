package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.EventStatus;
import com.prenota24.backend.domain.Reservation;
import com.prenota24.backend.domain.ReservationStatus;
import com.prenota24.backend.dto.CreateReservationRequest;
import com.prenota24.backend.dto.ReservationResponse;
import com.prenota24.backend.repository.EventRepository;
import com.prenota24.backend.repository.ReservationRepository;
import com.prenota24.backend.service.IReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService implements IReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ReservationResponse create(String slug, CreateReservationRequest request) {
        var event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("L'evento non è aperto alle prenotazioni");
        }

        if (reservationRepository.existsByEventIdAndGuestEmail(event.getId(), request.guestEmail())) {
            throw new IllegalArgumentException("Hai già una prenotazione per questo evento");
        }

        if (event.getMaxParticipants() != null) {
            long currentCount = reservationRepository.countByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED);
            if (currentCount >= event.getMaxParticipants()) {
                throw new IllegalArgumentException("I posti per questo evento sono esauriti");
            }
        }

        var reservation = Reservation.builder()
                .event(event)
                .guestName(request.guestName())
                .guestEmail(request.guestEmail())
                .guestPhone(request.guestPhone())
                .notes(request.notes())
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservation = reservationRepository.save(reservation);
        return toReservationResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getByEventId(UUID eventId) {
        return reservationRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(this::toReservationResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReservationResponse cancel(UUID reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Prenotazione non trovata"));

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);
        return toReservationResponse(reservation);
    }

    private ReservationResponse toReservationResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                reservation.getGuestPhone(),
                reservation.getNotes(),
                reservation.getStatus().toString(),
                reservation.getCreatedAt()
        );
    }
}
