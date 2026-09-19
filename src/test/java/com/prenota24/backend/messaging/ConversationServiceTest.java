package com.prenota24.backend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.prenota24.backend.common.MessagingConflictException;
import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.dto.SendMessageRequest;
import com.prenota24.backend.repository.*;
import com.prenota24.backend.service.impl.ConversationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationServiceTest {

    @Test
    void sameRequestIdAndPayloadReturnsOriginalButChangedPayloadConflicts() {
        var conversationRepository = mock(ConversationRepository.class);
        var messageRepository = mock(ConversationMessageRepository.class);
        var clientRepository = mock(ClientRepository.class);
        var studioId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var studio = Studio.builder().id(studioId).build();
        var client = Client.builder().id(UUID.randomUUID()).studio(studio).firstName("Ada").lastName("Lovelace")
                .phone("+39 333 1234567").build();
        var conversation = Conversation.builder().id(conversationId).studio(studio).client(client)
                .whatsappOptIn(true).lastInboundAt(Instant.parse("2026-09-19T11:00:00Z")).build();
        var sameRequest = new SendMessageRequest("Confermato", null, requestId, MessageKind.TEXT);
        var existing = ConversationMessage.builder().id(UUID.randomUUID()).conversation(conversation)
                .sequenceNo(42L)
                .text("Confermato").kind(MessageKind.TEXT).direction(MessageDirection.OUTBOUND)
                .status(MessageStatus.QUEUED).requestId(requestId)
                .requestFingerprint(MessageRequestFingerprint.calculate(sameRequest)).createdAt(Instant.parse("2026-09-19T11:30:00Z"))
                .build();
        when(conversationRepository.findByIdAndStudioId(conversationId, studioId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndRequestId(conversationId, requestId)).thenReturn(Optional.of(existing));
        when(clientRepository.findByNormalizedPhone(studioId, "393331234567")).thenReturn(List.of(client));

        var service = new ConversationService(
                conversationRepository,
                messageRepository,
                mock(ConversationReadCursorRepository.class),
                mock(ConversationConsentAuditRepository.class),
                mock(AppointmentRepository.class),
                clientRepository,
                mock(AppUserRepository.class),
                configuredProperties(studioId),
                Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC));

        assertThat(service.enqueue(conversationId, sameRequest, studioId, userId)).extracting("id").isEqualTo(existing.getId());
        assertThatThrownBy(() -> service.enqueue(conversationId,
                new SendMessageRequest("Testo cambiato", null, requestId, MessageKind.TEXT), studioId, userId))
                .isInstanceOf(MessagingConflictException.class);
    }

    private WhatsappProperties configuredProperties(UUID studioId) {
        return new WhatsappProperties(true, studioId, "phone-id", "token", "secret", "verify",
                "approved_template", "it", "v25.0", Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
