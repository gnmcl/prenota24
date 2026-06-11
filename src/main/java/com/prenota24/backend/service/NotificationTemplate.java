package com.prenota24.backend.service;

import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.dto.NotificationPayload;

/**
 * Strategy interface for building a single typed notification payload
 * from an Appointment domain object.
 *
 * Each implementation is responsible for exactly one notification type
 * and is auto-discovered by AppointmentNotificationFactory via Spring DI.
 */
public interface NotificationTemplate {

    /** The notification type this template produces. Must be unique across implementations. */
    NotificationType type();

    /**
     * Build the full notification payload for the given appointment.
     *
     * @param appointment the appointment triggering the notification
     * @return a NotificationPayload ready to be persisted and dispatched
     */
    NotificationPayload build(Appointment appointment);
}

