package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    List<Professional> findByStudioIdOrderByLastNameAsc(UUID studioId);

    List<Professional> findByStudioIdAndActiveTrue(UUID studioId);

    Optional<Professional> findByIdAndStudioId(UUID id, UUID studioId);
}
