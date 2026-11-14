package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStudioIdOrderByEventDateDesc(UUID studioId);

    Optional<Event> findBySlug(String slug);

    /**
     * Come {@link #findBySlug} ma blocca la riga dell'evento fino a fine transazione.
     * Serializza le prenotazioni concorrenti: senza il lock due richieste simultanee
     * leggevano lo stesso conteggio e superavano insieme il numero massimo di posti.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.slug = :slug")
    Optional<Event> findBySlugForUpdate(@Param("slug") String slug);

    List<Event> findByCreatedByIdOrderByEventDateDesc(UUID userId);
}
