package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EmailNormalizer;
import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Event;
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
        // Lock sulla riga dell'evento: controllo dei posti e inserimento diventano atomici.
        var event = eventRepository.findBySlugForUpdate(slug)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));
        var guestEmail = EmailNormalizer.normalize(request.guestEmail());

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("L'evento non è aperto alle prenotazioni");
        }

        if (reservationRepository.existsByEventIdAndGuestEmailIgnoreCase(event.getId(), guestEmail)) {
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
                .guestEmail(guestEmail)
                .guestPhone(request.guestPhone())
                .notes(request.notes())
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservation = reservationRepository.save(reservation);
        return toReservationResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getByEventId(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));
        // Stessa regola di EventService: solo chi ha creato l'evento vede le prenotazioni.
        // 404 invece di 403 per non rivelare l'esistenza dell'evento.
        if (!isOwner(event, userId)) {
            throw new EntityNotFoundException("Evento non trovato");
        }

        return reservationRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(this::toReservationResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReservationResponse cancel(UUID reservationId, UUID userId) {
        var reservation = reservationRepository.findById(reservationId)
                .filter(item -> isOwner(item.getEvent(), userId))
                .orElseThrow(() -> new EntityNotFoundException("Prenotazione non trovata"));

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);
        return toReservationResponse(reservation);
    }

    private boolean isOwner(Event event, UUID userId) {
        return event.getCreatedBy() != null && event.getCreatedBy().getId().equals(userId);
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
