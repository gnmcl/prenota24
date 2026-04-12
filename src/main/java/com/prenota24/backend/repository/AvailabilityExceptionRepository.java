package com.prenota24.backend.repository;

import com.prenota24.backend.domain.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {

    List<AvailabilityException> findByProfessionalIdAndDateBetween(UUID professionalId, LocalDate from, LocalDate to);

    Optional<AvailabilityException> findByProfessionalIdAndDate(UUID professionalId, LocalDate date);
}
