package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MessagingPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");

    @Test
    void freeTextRequiresConfigurationUniquePhoneConsentAndOpenWindow() {
        assertThat(MessagingPolicy.evaluate(true, true, true, true, NOW.minusSeconds(60), NOW).canSendText()).isTrue();
        assertThat(MessagingPolicy.evaluate(false, true, true, true, NOW.minusSeconds(60), NOW).blockedReason())
                .isEqualTo("WHATSAPP_NOT_CONFIGURED");
        assertThat(MessagingPolicy.evaluate(true, false, true, true, NOW.minusSeconds(60), NOW).blockedReason())
                .isEqualTo("CLIENT_PHONE_MISSING");
        assertThat(MessagingPolicy.evaluate(true, true, false, true, NOW.minusSeconds(60), NOW).blockedReason())
                .isEqualTo("CLIENT_PHONE_AMBIGUOUS");
        assertThat(MessagingPolicy.evaluate(true, true, true, false, NOW.minusSeconds(60), NOW).blockedReason())
                .isEqualTo("WHATSAPP_OPT_IN_REQUIRED");
        assertThat(MessagingPolicy.evaluate(true, true, true, true, NOW.minusSeconds(86_401), NOW).blockedReason())
                .isEqualTo("SERVICE_WINDOW_EXPIRED");
        assertThat(MessagingPolicy.evaluate(true, true, true, true, NOW.minusSeconds(86_400), NOW).canSendText())
                .isFalse();
    }

    @Test
    void templateDoesNotRequireOpenServiceWindow() {
        var result = MessagingPolicy.evaluate(true, true, true, true, null, NOW);

        assertThat(result.canSendText()).isFalse();
        assertThat(result.canSendTemplate()).isTrue();
        assertThat(result.serviceWindowExpiresAt()).isNull();
    }
}
