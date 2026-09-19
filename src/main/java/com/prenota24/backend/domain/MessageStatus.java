package com.prenota24.backend.domain;

public enum MessageStatus {
    RECEIVED,
    QUEUED,
    SENDING,
    ACCEPTED,
    DELIVERED,
    READ,
    FAILED,
    UNKNOWN;

    public boolean canAdvanceTo(MessageStatus next) {
        if (this == next) {
            return false;
        }
        if (next == DELIVERED || next == READ) {
            return switch (this) {
                case QUEUED, SENDING, ACCEPTED, UNKNOWN -> true;
                case DELIVERED -> next == READ;
                default -> false;
            };
        }
        if (next == ACCEPTED) {
            return this == QUEUED || this == SENDING || this == UNKNOWN;
        }
        if (next == FAILED) {
            return this == QUEUED || this == SENDING || this == ACCEPTED || this == UNKNOWN;
        }
        return false;
    }
}
