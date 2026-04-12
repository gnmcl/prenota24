package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByStudioId(UUID studioId, Pageable pageable);

    Optional<Appointment> findByToken(String token);

    Optional<Appointment> findByIdAndStudioId(UUID id, UUID studioId);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.id <> :excludeId
            AND a.status IN (com.prenota24.backend.domain.AppointmentStatus.REQUESTED,
                             com.prenota24.backend.domain.AppointmentStatus.CONFIRMED,
                             com.prenota24.backend.domain.AppointmentStatus.PROPOSED_NEW_TIME)
            AND a.startDatetime < :endTime
            AND a.endDatetime > :startTime
            """)
    long countConflictingAppointments(
            @Param("professionalId") UUID professionalId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.status IN (com.prenota24.backend.domain.AppointmentStatus.CONFIRMED,
                               com.prenota24.backend.domain.AppointmentStatus.REQUESTED)
            AND a.startDatetime >= :from
            AND a.startDatetime < :to
            """)
    List<Appointment> findForReminder(@Param("from") Instant from, @Param("to") Instant to);

    Page<Appointment> findByStudioIdAndProfessionalId(UUID studioId, UUID professionalId, Pageable pageable);
}
