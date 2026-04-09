package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Reservation;
import com.prenota24.backend.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByEventIdOrderByCreatedAtDesc(UUID eventId);

    long countByEventIdAndStatus(UUID eventId, ReservationStatus status);

    boolean existsByEventIdAndGuestEmail(UUID eventId, String guestEmail);

    void deleteAllByEventId(UUID eventId);
}
