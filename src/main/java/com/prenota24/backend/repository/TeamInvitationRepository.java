package com.prenota24.backend.repository;

import com.prenota24.backend.domain.InvitationStatus;
import com.prenota24.backend.domain.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, UUID> {

    Optional<TeamInvitation> findByToken(String token);

    List<TeamInvitation> findByStudioIdOrderByCreatedAtDesc(UUID studioId);

    Optional<TeamInvitation> findByProfessionalIdAndStatus(UUID professionalId, InvitationStatus status);

    Optional<TeamInvitation> findByIdAndStudioId(UUID id, UUID studioId);
}
