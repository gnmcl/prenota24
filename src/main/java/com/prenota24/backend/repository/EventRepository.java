package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStudioIdOrderByEventDateDesc(UUID studioId);

    Optional<Event> findBySlug(String slug);

    List<Event> findByCreatedByIdOrderByEventDateDesc(UUID userId);
}
