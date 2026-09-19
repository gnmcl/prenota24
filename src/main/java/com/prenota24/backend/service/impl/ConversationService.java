package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.*;
import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.messaging.*;
import com.prenota24.backend.repository.*;
import com.prenota24.backend.service.IConversationService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService implements IConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationReadCursorRepository readCursorRepository;
    private final ConversationConsentAuditRepository consentAuditRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final WhatsappProperties properties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> list(UUID studioId, UUID userId, Pageable pageable) {
        return conversationRepository.findForStudio(studioId, pageable)
                .map(conversation -> toConversationResponse(conversation, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse get(UUID conversationId, UUID studioId, UUID userId) {
        return toConversationResponse(findConversation(conversationId, studioId), userId);
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateForAppointment(UUID appointmentId, UUID studioId, UUID userId) {
        var appointment = appointmentRepository.findByIdAndStudioId(appointmentId, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Appuntamento non trovato"));
        conversationRepository.insertIfAbsent(UUID.randomUUID(), studioId, appointment.getClient().getId());
        var conversation = conversationRepository.findByStudioIdAndClientId(studioId, appointment.getClient().getId())
                .orElseThrow(() -> new IllegalStateException("Conversazione non creata"));
        return toConversationResponse(conversation, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(UUID conversationId, UUID studioId, Pageable pageable) {
        findConversation(conversationId, studioId);
        return messageRepository.findHistory(conversationId, studioId, pageable)
                .map(this::toMessageResponse);
    }

    @Override
    @Transactional
    public MessageResponse enqueue(UUID conversationId, SendMessageRequest request, UUID studioId, UUID userId) {
        var conversation = findConversation(conversationId, studioId);
        String fingerprint = MessageRequestFingerprint.calculate(request);
        var prior = messageRepository.findByConversationIdAndRequestId(conversationId, request.requestId());
        if (prior.isPresent()) {
            return sameRequestOrThrow(prior.get(), fingerprint);
        }

        validateSendRequest(request);
        var policy = policyFor(conversation);
        boolean allowed = request.kind() == MessageKind.TEXT ? policy.canSendText() : policy.canSendTemplate();
        if (!allowed) {
            throw new MessageSendBlockedException(policy.blockedReason());
        }
        var sender = appUserRepository.findById(userId)
                .filter(item -> item.getStudio().getId().equals(studioId))
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        Appointment appointment = null;
        if (request.appointmentId() != null) {
            appointment = appointmentRepository.findByIdAndStudioId(request.appointmentId(), studioId)
                    .filter(item -> item.getClient().getId().equals(conversation.getClient().getId()))
                    .orElseThrow(() -> new EntityNotFoundException("Appuntamento non associato al cliente della conversazione"));
        }

        String storedText = request.kind() == MessageKind.TEXT ? request.text().trim() : properties.templateName();
        String recipientPhone = PhoneNormalizer.normalizeClientPhone(conversation.getClient().getPhone()).orElseThrow();
        UUID messageId = UUID.randomUUID();
        int inserted = messageRepository.insertQueuedIfAbsent(messageId, conversationId,
                appointment == null ? null : appointment.getId(), sender.getId(), request.requestId(), fingerprint,
                recipientPhone, storedText, request.kind().name(), senderDisplayName(sender));
        var message = inserted == 1
                ? messageRepository.findById(messageId).orElseThrow()
                : messageRepository.findByConversationIdAndRequestId(conversationId, request.requestId()).orElseThrow();
        if (inserted == 0) {
            return sameRequestOrThrow(message, fingerprint);
        }
        conversationRepository.updateActivityIfNewer(conversationId, message.getSequenceNo(),
                message.getCreatedAt(), preview(storedText));
        return toMessageResponse(message);
    }

    @Override
    @Transactional
    public void markRead(UUID conversationId, UUID messageId, UUID studioId, UUID userId) {
        findConversation(conversationId, studioId);
        var message = messageRepository.findByIdAndConversationIdAndConversationStudioId(messageId, conversationId, studioId)
                .filter(item -> item.getDirection() == MessageDirection.INBOUND)
                .orElseThrow(() -> new IllegalArgumentException("Il cursore può avanzare solo su un messaggio in entrata visualizzato"));
        var user = appUserRepository.findById(userId)
                .filter(item -> item.getStudio().getId().equals(studioId))
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
        readCursorRepository.advance(conversationId, user.getId(), message.getId(),
                message.getCreatedAt(), message.getSequenceNo());
    }

    @Override
    @Transactional
    public ConversationResponse updateConsent(UUID conversationId, boolean enabled, UUID studioId, UUID userId) {
        var conversation = findConversation(conversationId, studioId);
        var user = appUserRepository.findById(userId)
                .filter(item -> item.getStudio().getId().equals(studioId))
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
        String normalizedPhone;
        if (enabled) {
            normalizedPhone = PhoneNormalizer.normalizeClientPhone(conversation.getClient().getPhone())
                    .orElseThrow(() -> new MessageSendBlockedException("CLIENT_PHONE_INVALID"));
            if (clientRepository.findByNormalizedPhone(studioId, normalizedPhone).size() != 1) {
                throw new MessageSendBlockedException("CLIENT_PHONE_AMBIGUOUS");
            }
        } else {
            normalizedPhone = PhoneNormalizer.normalizeClientPhone(conversation.getClient().getPhone())
                    .orElse(conversation.getWhatsappOptInPhone());
        }
        conversation.setWhatsappOptIn(enabled);
        conversation.setWhatsappOptInPhone(enabled ? normalizedPhone : null);
        consentAuditRepository.save(ConversationConsentAudit.builder()
                .conversation(conversation).user(user).enabled(enabled).recipientPhone(normalizedPhone).build());
        return toConversationResponse(conversation, userId);
    }

    private Conversation findConversation(UUID id, UUID studioId) {
        return conversationRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Conversazione non trovata"));
    }

    private MessagingPolicy.Result policyFor(Conversation conversation) {
        String rawPhone = conversation.getClient().getPhone();
        var normalizedResult = PhoneNormalizer.normalizeClientPhone(rawPhone);
        boolean configured = properties.isConfiguredFor(conversation.getStudio().getId());
        if (!configured) {
            return MessagingPolicy.evaluate(false, normalizedResult.isPresent(), false, false, null, clock.instant());
        }
        if (rawPhone != null && !rawPhone.isBlank() && normalizedResult.isEmpty()) {
            return new MessagingPolicy.Result(false, false, "CLIENT_PHONE_INVALID", null);
        }
        String normalized = normalizedResult.orElse("");
        boolean hasPhone = !normalized.isBlank();
        boolean unique = hasPhone && clientRepository.findByNormalizedPhone(conversation.getStudio().getId(), normalized).size() == 1;
        boolean optedInForPhone = conversation.isWhatsappOptIn()
                && normalized.equals(conversation.getWhatsappOptInPhone());
        Instant inboundForPhone = normalized.equals(conversation.getLastInboundPhone())
                ? conversation.getLastInboundAt() : null;
        return MessagingPolicy.evaluate(true, hasPhone, unique,
                optedInForPhone, inboundForPhone, clock.instant());
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID userId) {
        var policy = policyFor(conversation);
        Long cursorSequence = readCursorRepository.findByConversationIdAndUserId(conversation.getId(), userId)
                .map(ConversationReadCursor::getLastReadSequenceNo).orElse(null);
        long unread = cursorSequence == null
                ? messageRepository.countByConversationIdAndDirection(conversation.getId(), MessageDirection.INBOUND)
                : messageRepository.countUnread(conversation.getId(), MessageDirection.INBOUND, cursorSequence);
        var client = conversation.getClient();
        return new ConversationResponse(conversation.getId(), client.getId(),
                (client.getFirstName() + " " + client.getLastName()).trim(), client.getPhone(),
                conversation.getLastMessagePreview(), conversation.getLastMessageAt(), unread,
                properties.isConfiguredFor(conversation.getStudio().getId()),
                conversation.isWhatsappOptIn() && normalizedPhoneMatchesOptIn(conversation),
                policy.canSendText(), policy.canSendTemplate(), policy.blockedReason(), policy.serviceWindowExpiresAt());
    }

    private MessageResponse sameRequestOrThrow(ConversationMessage message, String fingerprint) {
        if (!fingerprint.equals(message.getRequestFingerprint())) {
            throw new MessagingConflictException("requestId già usato con un payload diverso");
        }
        return toMessageResponse(message);
    }

    private void validateSendRequest(SendMessageRequest request) {
        if (request.kind() == MessageKind.UNSUPPORTED) {
            throw new IllegalArgumentException("UNSUPPORTED non è inviabile");
        }
        if (request.kind() == MessageKind.TEXT && (request.text() == null || request.text().isBlank())) {
            throw new IllegalArgumentException("text è obbligatorio per i messaggi TEXT");
        }
        if (request.kind() == MessageKind.TEMPLATE && request.text() != null && !request.text().isBlank()) {
            throw new IllegalArgumentException("I template V1 non accettano testo o parametri arbitrari");
        }
    }

    private MessageResponse toMessageResponse(ConversationMessage message) {
        return new MessageResponse(message.getId(), message.getSequenceNo(), message.getConversation().getId(), message.getText(),
                message.getDirection(), message.getKind(), message.getStatus(), message.getCreatedAt(),
                message.getAppointment() == null ? null : message.getAppointment().getId(),
                message.getSenderName(), message.getErrorMessage());
    }

    private String preview(String text) {
        return text.length() <= 240 ? text : text.substring(0, 240);
    }

    private String senderDisplayName(AppUser sender) {
        return sender.getName() == null || sender.getName().isBlank() ? sender.getEmail() : sender.getName();
    }

    private boolean normalizedPhoneMatchesOptIn(Conversation conversation) {
        return PhoneNormalizer.normalizeClientPhone(conversation.getClient().getPhone())
                .map(phone -> phone.equals(conversation.getWhatsappOptInPhone()))
                .orElse(false);
    }
}
