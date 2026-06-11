package com.prenota24.backend.dto;

import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationType;
import com.prenota24.backend.domain.RecipientType;

import java.util.UUID;

/**
 * Wraps all metadata needed to persist and dispatch one notification.
 * Built by AppointmentNotificationFactory; consumed by NotificationService.
 */
public record NotificationPayload(
        NotificationType type,
        NotificationChannel channel,
        RecipientType recipientType,
        UUID recipientId,
        EmailPayload email
) {}

