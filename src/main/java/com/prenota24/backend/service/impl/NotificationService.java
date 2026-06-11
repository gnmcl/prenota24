package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.AppointmentAction;
import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.dto.NotificationPayload;
import com.prenota24.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Responsible for:
 * 1. Persisting Notification entities
 * 2. Dispatching them immediately after transaction commit
 *
 * No email content, no String.format(), no switch on action types.
 * All payload building is delegated to AppointmentNotificationFactory.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final AppointmentNotificationFactory notificationFactory;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Schedule and immediately dispatch all notifications for an appointment action.
     * Called after a state machine transition (confirm, cancel, propose, etc.).
     */
    @Transactional
    public void scheduleForTransition(Appointment apt, AppointmentAction action) {
        var payloads = notificationFactory.buildForAction(apt, action);
        payloads.forEach(p -> scheduleAndDispatch(apt, p));
    }

    /**
     * Schedule the 24h reminder for an appointment.
     * The reminder is scheduled 24h before the appointment start; the dispatcher
     * will pick it up at the right time via ReminderScheduler.processPending().
     */
    @Transactional
    public Notification scheduleReminder(Appointment apt, NotificationChannel channel) {
        var payload = notificationFactory.build(NotificationType.REMINDER_24H, apt);
        var notification = Notification.builder()
                .studio(apt.getStudio())
                .appointment(apt)
                .channel(payload.channel())
                .type(payload.type().name())
                .recipientId(payload.recipientId())
                .recipientType(payload.recipientType())
                .scheduledAt(apt.getStartDatetime().minus(24, ChronoUnit.HOURS))
                .payload(payload.email().toMap())
                .build();
        return notificationRepository.save(notification);
        // No immediate dispatch: reminder is scheduled in the future,
        // ReminderScheduler.dispatchPending() will send it at the right time.
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void scheduleAndDispatch(Appointment apt, NotificationPayload payload) {
        var notification = Notification.builder()
                .studio(apt.getStudio())
                .appointment(apt)
                .channel(payload.channel())
                .type(payload.type().name())
                .recipientId(payload.recipientId())
                .recipientType(payload.recipientType())
                .scheduledAt(Instant.now())
                .payload(payload.email().toMap())
                .build();

        var saved = notificationRepository.save(notification);
        dispatchAfterCommit(saved);
    }

    /**
     * Registers a post-commit callback so the async dispatcher reads a fully
     * committed row — avoids the race condition where the async thread queries
     * a not-yet-visible notification.
     */
    private void dispatchAfterCommit(Notification notification) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationDispatcher.dispatch(notification);
                }
            });
        } else {
            // Outside a transaction (e.g. tests): dispatch immediately
            notificationDispatcher.dispatch(notification);
        }
    }
}
