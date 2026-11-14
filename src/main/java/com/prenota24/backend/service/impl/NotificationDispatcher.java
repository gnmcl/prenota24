package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationStatus;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.repository.NotificationRepository;
import com.prenota24.backend.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationDispatcher(NotificationRepository notificationRepository,
                                   List<NotificationSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    @Async
    public void dispatch(Notification notification) {
        var sender = senders.get(notification.getChannel());
        if (sender == null) {
            logger.warn("Nessun sender trovato per canale {}, notifica {} skipped",
                    notification.getChannel(), notification.getId());
            notification.setStatus(NotificationStatus.SKIPPED);
            notificationRepository.save(notification);
            return;
        }

        try {
            sender.send(notification);
        } catch (Exception e) {
            logger.error("Dispatch fallito per notifica {}: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * Process all pending notifications that are ready to be sent.
     */
    public void processPending() {
        var pending = notificationRepository.findPendingToSend(Instant.now());
        for (var notification : pending) {
            if (isStaleReminder(notification)) {
                logger.info("Promemoria {} non inviato: appuntamento annullato o spostato", notification.getId());
                notification.setStatus(NotificationStatus.SKIPPED);
                notificationRepository.save(notification);
                continue;
            }
            dispatch(notification);
        }
    }

    /**
     * Il promemoria viene creato circa 24 ore prima e inviato più tardi: nel frattempo
     * l'appuntamento può essere stato annullato, rifiutato, completato o spostato.
     * In quei casi il promemoria è da scartare (per uno spostato ne verrà creato uno nuovo).
     */
    static boolean isStaleReminder(Notification notification) {
        if (!NotificationType.REMINDER_24H.name().equals(notification.getType())) {
            return false;
        }
        var appointment = notification.getAppointment();
        if (appointment == null) {
            return false;
        }
        return !ReminderScheduler.REMINDABLE_STATUSES.contains(appointment.getStatus())
                || !ReminderScheduler.reminderTimeFor(appointment).equals(notification.getScheduledAt());
    }
}
