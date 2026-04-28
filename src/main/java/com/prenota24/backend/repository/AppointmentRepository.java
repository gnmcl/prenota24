package com.prenota24.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentStatus;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    Page<Appointment> findByStudioId(UUID studioId, Pageable pageable);

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.studio.id = :studioId
            AND a.startDatetime >= :from
            AND a.startDatetime < :to
            """)
    Page<Appointment> findByStudioIdAndDateRange(
            @Param("studioId") UUID studioId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.studio.id = :studioId
            AND a.status = :status
            AND a.startDatetime >= :from
            AND a.startDatetime < :to
            """)
    Page<Appointment> findByStudioIdAndStatusAndDateRange(
            @Param("studioId") UUID studioId,
            @Param("status") AppointmentStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    Optional<Appointment> findByToken(String token);

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
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

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    Page<Appointment> findByStudioIdAndProfessionalId(UUID studioId, UUID professionalId, Pageable pageable);

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    Page<Appointment> findByStudioIdAndStatus(UUID studioId, AppointmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"professional", "client", "serviceType"})
    Page<Appointment> findByStudioIdAndStatusAndProfessionalId(UUID studioId, AppointmentStatus status, UUID professionalId, Pageable pageable);

    List<Appointment> findByClientIdAndStudioIdOrderByStartDatetimeDesc(UUID clientId, UUID studioId);

    // ── Professional Portal queries ──────────────────────────
    Page<Appointment> findByProfessionalId(UUID professionalId, Pageable pageable);

    Page<Appointment> findByProfessionalIdAndStatus(UUID professionalId, AppointmentStatus status, Pageable pageable);

    Optional<Appointment> findByIdAndProfessionalId(UUID id, UUID professionalId);

    long countByProfessionalIdAndStatusIn(UUID professionalId, java.util.Collection<AppointmentStatus> statuses);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.startDatetime >= :from
            AND a.startDatetime < :to
            AND a.status IN (com.prenota24.backend.domain.AppointmentStatus.CONFIRMED,
                             com.prenota24.backend.domain.AppointmentStatus.REQUESTED)
            """)
    long countTodayAppointments(@Param("professionalId") UUID professionalId,
                                @Param("from") Instant from,
                                @Param("to") Instant to);
}
