package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

    List<Availability> findByProfessionalId(UUID professionalId);

    List<Availability> findByProfessionalIdAndDayOfWeek(UUID professionalId, short dayOfWeek);

    void deleteByProfessionalId(UUID professionalId);
}
