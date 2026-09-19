package com.prenota24.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.messaging.PhoneNormalizer;
import com.prenota24.backend.messaging.WebhookSignatureVerifier;
import com.prenota24.backend.repository.*;
import com.prenota24.backend.service.IWhatsappWebhookService;
import java.time.Instant;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WhatsappWebhookService implements IWhatsappWebhookService {
    private final ObjectMapper objectMapper;
    private final WhatsappProperties properties;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ClientRepository clientRepository;
    private final WhatsappInboundQuarantineRepository quarantineRepository;
    private final Clock clock;

    @Override
    public String verifyChallenge(String mode, String token, String challenge) {
        ensureEnabled();
        if ("subscribe".equals(mode) && properties.verifyToken().equals(token) && challenge != null) {
            return challenge;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verifica webhook rifiutata");
    }

    @Override
    @Transactional
    public void receiveSigned(String rawBody, String signature) {
        ensureEnabled();
        if (!new WebhookSignatureVerifier(properties.appSecret()).isValid(rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma webhook non valida");
        }
        process(rawBody);
    }

    public ProcessingResult process(String rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Payload webhook non valido", ex);
        }
        var totals = new MutableResult();
        for (var entry : root.path("entry")) {
            for (var change : entry.path("changes")) {
                processValue(change.path("value"), rawBody, totals);
            }
        }
        return totals.freeze();
    }

    private void processValue(JsonNode value, String rawBody, MutableResult totals) {
        String destinationId = value.path("metadata").path("phone_number_id").asText("");
        for (var message : value.path("messages")) {
            processIncoming(message, value, destinationId, rawBody, totals);
        }
        if (properties.phoneNumberId().equals(destinationId)) {
            for (var status : value.path("statuses")) {
                processStatus(status, totals);
            }
        }
    }

    private void processIncoming(JsonNode message, JsonNode value, String destinationId,
                                 String rawBody, MutableResult totals) {
        String providerId = message.path("id").asText("");
        if (providerId.isBlank()) return;
        if (messageRepository.existsByProviderMessageId(providerId)
                || quarantineRepository.existsByProviderMessageId(providerId)) {
            totals.duplicates++;
            return;
        }
        var senderPhoneResult = PhoneNormalizer.normalizeProviderPhone(message.path("from").asText(""));
        String senderPhone = senderPhoneResult.orElse("");
        String type = message.path("type").asText("unsupported");
        String text = "text".equals(type)
                ? message.path("text").path("body").asText("")
                : "[Messaggio " + type + " non supportato]";
        if (senderPhoneResult.isEmpty()) {
            totals.quarantined += quarantine(providerId, destinationId, message.path("from").asText(""), text,
                    "INVALID_PHONE", rawBody);
            return;
        }
        if (!properties.phoneNumberId().equals(destinationId)) {
            totals.quarantined += quarantine(providerId, destinationId, senderPhone, text,
                    "DESTINATION_MISMATCH", rawBody);
            return;
        }
        var parsedTimestamp = parseTimestamp(message.path("timestamp").asText());
        if (parsedTimestamp.isEmpty() || parsedTimestamp.get().isAfter(clock.instant())) {
            totals.quarantined += quarantine(providerId, destinationId, senderPhone, text,
                    "INVALID_TIMESTAMP", rawBody);
            return;
        }
        Instant createdAt = parsedTimestamp.get();
        var clients = clientRepository.findByNormalizedPhone(properties.pilotStudioId(), senderPhone);
        if (clients.size() != 1) {
            totals.quarantined += quarantine(providerId, destinationId, senderPhone, text,
                    clients.isEmpty() ? "UNMATCHED_PHONE" : "AMBIGUOUS_PHONE", rawBody);
            return;
        }
        var client = clients.getFirst();
        conversationRepository.insertIfAbsent(UUID.randomUUID(), properties.pilotStudioId(), client.getId());
        var conversation = conversationRepository.findByStudioIdAndClientId(properties.pilotStudioId(), client.getId())
                .orElseThrow(() -> new IllegalStateException("Conversazione inbound non creata"));
        String senderName = senderName(value, senderPhone);
        MessageKind kind = "text".equals(type) ? MessageKind.TEXT : MessageKind.UNSUPPORTED;
        int inserted = messageRepository.insertInboundIfAbsent(UUID.randomUUID(), conversation.getId(), providerId,
                text, kind.name(), senderName, createdAt);
        if (inserted == 0) {
            totals.duplicates++;
            return;
        }
        var persisted = messageRepository.findByProviderMessageId(providerId).orElseThrow();
        conversationRepository.updateInboundActivity(conversation.getId(), persisted.getSequenceNo(),
                createdAt, senderPhone, createdAt, preview(text));
        totals.received++;
    }

    private void processStatus(JsonNode statusNode, MutableResult totals) {
        String providerId = statusNode.path("id").asText("");
        MessageStatus next = switch (statusNode.path("status").asText("")) {
            case "sent" -> MessageStatus.ACCEPTED;
            case "delivered" -> MessageStatus.DELIVERED;
            case "read" -> MessageStatus.READ;
            case "failed" -> MessageStatus.FAILED;
            default -> null;
        };
        if (next == null) return;
        var message = messageRepository.findByProviderMessageId(providerId)
                .or(() -> messageByOpaqueCallback(statusNode));
        var recipient = PhoneNormalizer.normalizeProviderPhone(statusNode.path("recipient_id").asText(""));
        message.filter(item -> item.getDirection() == MessageDirection.OUTBOUND)
                .filter(item -> item.getConversation().getStudio().getId().equals(properties.pilotStudioId()))
                .filter(item -> recipient.filter(phone -> phone.equals(item.getRecipientPhone())).isPresent())
                .ifPresent(item -> totals.statuses += messageRepository.advanceProviderStatus(item.getId(), next.name(),
                        providerId.isBlank() ? null : providerId,
                        next == MessageStatus.FAILED ? statusError(statusNode) : null));
    }

    private int quarantine(String providerId, String destinationId, String senderPhone,
                           String text, String reason, String rawBody) {
        return quarantineRepository.insertIfAbsent(providerId, destinationId, senderPhone, text, reason, rawBody);
    }

    private String senderName(JsonNode value, String senderPhone) {
        for (var contact : value.path("contacts")) {
            if (PhoneNormalizer.normalizeProviderPhone(contact.path("wa_id").asText())
                    .filter(senderPhone::equals).isPresent()) {
                return contact.path("profile").path("name").asText(null);
            }
        }
        return null;
    }

    private Optional<ConversationMessage> messageByOpaqueCallback(JsonNode statusNode) {
        String opaque = statusNode.path("biz_opaque_callback_data").asText("");
        try {
            return opaque.isBlank() ? Optional.empty() : messageRepository.findById(UUID.fromString(opaque));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<Instant> parseTimestamp(String value) {
        try {
            return Optional.of(Instant.ofEpochSecond(Long.parseLong(value)));
        } catch (NumberFormatException | java.time.DateTimeException ex) {
            return Optional.empty();
        }
    }

    private String statusError(JsonNode statusNode) {
        var error = statusNode.path("errors").path(0);
        String title = error.path("title").asText("");
        String message = error.path("message").asText("");
        String combined = (title + (title.isBlank() || message.isBlank() ? "" : ": ") + message).trim();
        return combined.isBlank() ? "Invio rifiutato da WhatsApp" : combined;
    }

    private String preview(String value) { return value.length() <= 240 ? value : value.substring(0, 240); }

    private void ensureEnabled() {
        if (!properties.enabled() || !properties.isConfiguredFor(properties.pilotStudioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook WhatsApp disabilitato");
        }
    }

    public record ProcessingResult(int received, int duplicates, int quarantined, int statuses) {}
    private static final class MutableResult {
        int received;
        int duplicates;
        int quarantined;
        int statuses;
        ProcessingResult freeze() { return new ProcessingResult(received, duplicates, quarantined, statuses); }
    }
}
