package com.prenota24.backend.repository;

import com.prenota24.backend.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByAppointmentId(UUID appointmentId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.status = com.prenota24.backend.domain.NotificationStatus.PENDING
            AND n.scheduledAt <= :now
            ORDER BY n.scheduledAt ASC
            """)
    List<Notification> findPendingToSend(@Param("now") Instant now);
}
