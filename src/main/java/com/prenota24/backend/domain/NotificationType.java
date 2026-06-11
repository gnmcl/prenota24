package com.prenota24.backend.domain;

/**
 * Domain-level notification type identifiers.
 * Replaces magic strings in Notification.type column.
 */
public enum NotificationType {
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CANCELLED_BY_PROFESSIONAL,
    APPOINTMENT_CANCELLED_BY_CLIENT,
    REMINDER_24H,
    PROPOSAL_RECEIVED,
    PROPOSAL_ACCEPTED
}

