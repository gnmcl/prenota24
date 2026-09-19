package com.prenota24.backend.messaging;

import java.time.Duration;
import java.time.Instant;

public final class MessagingPolicy {

    private static final Duration SERVICE_WINDOW = Duration.ofHours(24);

    private MessagingPolicy() {}

    public static Result evaluate(boolean configured,
                                  boolean hasPhone,
                                  boolean phoneIsUnique,
                                  boolean optedIn,
                                  Instant lastInboundAt,
                                  Instant now) {
        Instant expiresAt = lastInboundAt == null ? null : lastInboundAt.plus(SERVICE_WINDOW);
        String commonBlock = null;
        if (!configured) {
            commonBlock = "WHATSAPP_NOT_CONFIGURED";
        } else if (!hasPhone) {
            commonBlock = "CLIENT_PHONE_MISSING";
        } else if (!phoneIsUnique) {
            commonBlock = "CLIENT_PHONE_AMBIGUOUS";
        } else if (!optedIn) {
            commonBlock = "WHATSAPP_OPT_IN_REQUIRED";
        }
        if (commonBlock != null) {
            return new Result(false, false, commonBlock, expiresAt);
        }
        boolean windowOpen = expiresAt != null && now.isBefore(expiresAt);
        return new Result(windowOpen, true, windowOpen ? null : "SERVICE_WINDOW_EXPIRED", expiresAt);
    }

    public record Result(
            boolean canSendText,
            boolean canSendTemplate,
            String blockedReason,
            Instant serviceWindowExpiresAt
    ) {}
}
