package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.ConversationMessageRepository;
import com.prenota24.backend.service.impl.MessageDispatchService;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageDispatchServiceTest {

    @Test
    void dispatchRechecksTextWindowAndFailsClosedBeforeNetworkSend() {
        var messages = mock(ConversationMessageRepository.class);
        var clients = mock(ClientRepository.class);
        var studioId = UUID.randomUUID();
        var studio = Studio.builder().id(studioId).build();
        var client = Client.builder().id(UUID.randomUUID()).studio(studio).phone("+39 333 1112222").build();
        var conversation = Conversation.builder().id(UUID.randomUUID()).studio(studio).client(client)
                .whatsappOptIn(true).whatsappOptInPhone("393331112222")
                .lastInboundPhone("393331112222").lastInboundAt(Instant.parse("2026-09-18T11:59:59Z")).build();
        var message = ConversationMessage.builder().id(UUID.randomUUID()).conversation(conversation)
                .direction(MessageDirection.OUTBOUND).kind(MessageKind.TEXT).status(MessageStatus.SENDING).text("ciao")
                .recipientPhone("393331112222").build();
        when(messages.claimForDispatch(message.getId(), Instant.parse("2026-09-19T12:00:00Z"))).thenReturn(1);
        when(messages.findById(message.getId())).thenReturn(Optional.of(message));
        when(clients.findByNormalizedPhone(studioId, "393331112222")).thenReturn(List.of(client));

        var service = new MessageDispatchService(messages, clients, properties(studioId),
                Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC));

        assertThat(service.claim(message.getId())).isEmpty();
        assertThat(message.getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(message.getErrorMessage()).isEqualTo("SERVICE_WINDOW_EXPIRED");
    }

    @Test
    void ambiguousTransportResultIsCompletedAsUnknown() {
        var messages = mock(ConversationMessageRepository.class);
        var message = ConversationMessage.builder().id(UUID.randomUUID()).status(MessageStatus.SENDING).build();
        when(messages.completeDispatch(message.getId(), "UNKNOWN", null, "timeout after request")).thenReturn(1);
        var service = new MessageDispatchService(messages, mock(ClientRepository.class), properties(UUID.randomUUID()), Clock.systemUTC());

        int updated = service.complete(message.getId(), WhatsappGateway.SendResult.unknown("timeout after request"));

        assertThat(updated).isEqualTo(1);
    }

    private WhatsappProperties properties(UUID studioId) {
        return new WhatsappProperties(true, studioId, "phone-id", "token", "secret", "verify",
                "approved", "it", "v25.0", Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
