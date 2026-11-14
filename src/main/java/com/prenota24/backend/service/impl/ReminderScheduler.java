package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentStatus;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationStatus;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    /** Stati per cui ha senso ricordare l'appuntamento (gli stessi di findForReminder). */
    static final Set<AppointmentStatus> REMINDABLE_STATUSES =
            EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.REQUESTED);

    /** Momento di invio del promemoria: 24 ore prima dell'inizio. */
    static Instant reminderTimeFor(Appointment appointment) {
        return appointment.getStartDatetime().minus(24, ChronoUnit.HOURS);
    }

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * Every hour, schedule 24h reminders for upcoming appointments.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void scheduleReminders() {
        Instant from = Instant.now().plus(23, ChronoUnit.HOURS);
        Instant to = Instant.now().plus(25, ChronoUnit.HOURS);

        var upcoming = appointmentRepository.findForReminder(from, to);

        for (var apt : upcoming) {
            // Conta solo un promemoria valido per l'orario attuale: se l'appuntamento è stato
            // spostato, il vecchio viene scartato dal dispatcher e qui se ne crea uno nuovo.
            var expectedAt = reminderTimeFor(apt);
            boolean alreadyScheduled = notificationRepository
                    .findByAppointmentId(apt.getId())
                    .stream()
                    .anyMatch(n -> NotificationType.REMINDER_24H.name().equals(n.getType())
                            && n.getStatus() != NotificationStatus.SKIPPED
                            && expectedAt.equals(n.getScheduledAt()));

            if (!alreadyScheduled && apt.getClient().getEmail() != null) {
                notificationService.scheduleReminder(apt, NotificationChannel.EMAIL);
                logger.info("Reminder 24h schedulato per appuntamento {}", apt.getId());
            }
        }
    }

    /**
     * Every 5 minutes, process pending notifications.
     */
    @Scheduled(fixedRate = 300_000)
    public void dispatchPending() {
        notificationDispatcher.processPending();
    }
}
