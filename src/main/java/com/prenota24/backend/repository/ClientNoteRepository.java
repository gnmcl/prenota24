package com.prenota24.backend.repository;

import com.prenota24.backend.domain.ClientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientNoteRepository extends JpaRepository<ClientNote, UUID> {

    List<ClientNote> findByClientIdOrderByPinnedDescCreatedAtDesc(UUID clientId);

    Optional<ClientNote> findByIdAndStudioId(UUID id, UUID studioId);
}
