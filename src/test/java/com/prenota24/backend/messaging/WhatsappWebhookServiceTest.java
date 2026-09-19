package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.Client;
import com.prenota24.backend.domain.Conversation;
import com.prenota24.backend.domain.ConversationMessage;
import com.prenota24.backend.domain.MessageDirection;
import com.prenota24.backend.domain.MessageStatus;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.repository.*;
import com.prenota24.backend.service.impl.WhatsappWebhookService;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhatsappWebhookServiceTest {
    private static final String PAYLOAD = """
            {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"dest-1"},
            "contacts":[{"wa_id":"393331112222","profile":{"name":"Mario"}}],
            "messages":[{"id":"wamid.1","from":"393331112222","timestamp":"1789820000","type":"text","text":{"body":"Buongiorno"}}]}}]}]}
            """;

    @Test
    void duplicateInboundWebhookIsReportedWithoutCreatingAnotherMessage() {
        var messages = mock(ConversationMessageRepository.class);
        when(messages.existsByProviderMessageId("wamid.1")).thenReturn(true);
        var service = service(messages, mock(ClientRepository.class), mock(WhatsappInboundQuarantineRepository.class));

        var result = service.process(PAYLOAD);

        assertThat(result.received()).isZero();
        assertThat(result.duplicates()).isEqualTo(1);
        assertThat(result.quarantined()).isZero();
    }

    @Test
    void ambiguousPhoneIsQuarantinedAndNeverLinked() {
        var messages = mock(ConversationMessageRepository.class);
        var clients = mock(ClientRepository.class);
        var quarantine = mock(WhatsappInboundQuarantineRepository.class);
        when(messages.existsByProviderMessageId("wamid.1")).thenReturn(false);
        when(quarantine.existsByProviderMessageId("wamid.1")).thenReturn(false);
        when(clients.findByNormalizedPhone(pilotStudioId(), "393331112222"))
                .thenReturn(List.of(Client.builder().build(), Client.builder().build()));
        when(quarantine.insertIfAbsent("wamid.1", "dest-1", "393331112222", "Buongiorno",
                "AMBIGUOUS_PHONE", PAYLOAD)).thenReturn(1);
        var service = service(messages, clients, quarantine);

        var result = service.process(PAYLOAD);

        assertThat(result.received()).isZero();
        assertThat(result.quarantined()).isEqualTo(1);
    }

    @Test
    void statusBeforeHttpCompletionCorrelatesThroughOpaqueLocalMessageId() {
        var messages = mock(ConversationMessageRepository.class);
        var localId = UUID.randomUUID();
        var studio = Studio.builder().id(pilotStudioId()).build();
        var message = ConversationMessage.builder().id(localId)
                .conversation(Conversation.builder().studio(studio).build())
                .direction(MessageDirection.OUTBOUND).status(MessageStatus.SENDING)
                .recipientPhone("393331112222").build();
        String payload = """
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"dest-1"},
                "statuses":[{"id":"wamid.early","status":"delivered","recipient_id":"393331112222","biz_opaque_callback_data":"%s"}]}}]}]}
                """.formatted(localId);
        when(messages.findByProviderMessageId("wamid.early")).thenReturn(Optional.empty());
        when(messages.findById(localId)).thenReturn(Optional.of(message));
        when(messages.advanceProviderStatus(localId, "DELIVERED", "wamid.early", null)).thenReturn(1);

        var result = service(messages, mock(ClientRepository.class), mock(WhatsappInboundQuarantineRepository.class))
                .process(payload);

        assertThat(result.statuses()).isEqualTo(1);
    }

    private WhatsappWebhookService service(ConversationMessageRepository messages,
                                            ClientRepository clients,
                                            WhatsappInboundQuarantineRepository quarantine) {
        return new WhatsappWebhookService(new ObjectMapper(), properties(), mock(ConversationRepository.class),
                messages, clients, quarantine,
                Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC));
    }

    private WhatsappProperties properties() {
        return new WhatsappProperties(true, pilotStudioId(), "dest-1", "token", "secret", "verify",
                "approved", "it", "v25.0", Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    private UUID pilotStudioId() {
        return UUID.fromString("48ad0c29-e75a-4bb4-b025-22c2b728dbd0");
    }
}
