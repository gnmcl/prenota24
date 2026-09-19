package com.prenota24.backend.service.impl;

import com.prenota24.backend.config.WhatsappProperties;
import com.prenota24.backend.domain.*;
import com.prenota24.backend.messaging.*;
import com.prenota24.backend.repository.ClientRepository;
import com.prenota24.backend.repository.ConversationMessageRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageDispatchService {
    private final ConversationMessageRepository messageRepository;
    private final ClientRepository clientRepository;
    private final WhatsappProperties properties;
    private final Clock clock;

    @Transactional
    public Optional<WhatsappGateway.DispatchCommand> claim(UUID messageId) {
        if (messageRepository.claimForDispatch(messageId, clock.instant()) != 1) {
            return Optional.empty();
        }
        var message = messageRepository.findById(messageId).orElseThrow();
        var conversation = message.getConversation();
        String phone = PhoneNormalizer.normalizeClientPhone(conversation.getClient().getPhone()).orElse("");
        if (!phone.equals(message.getRecipientPhone())) {
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage("CLIENT_PHONE_CHANGED");
            return Optional.empty();
        }
        boolean unique = !phone.isBlank()
                && clientRepository.findByNormalizedPhone(conversation.getStudio().getId(), phone).size() == 1;
        var policy = MessagingPolicy.evaluate(properties.isConfiguredFor(conversation.getStudio().getId()),
                !phone.isBlank(), unique,
                conversation.isWhatsappOptIn() && phone.equals(conversation.getWhatsappOptInPhone()),
                phone.equals(conversation.getLastInboundPhone()) ? conversation.getLastInboundAt() : null,
                clock.instant());
        boolean allowed = message.getKind() == MessageKind.TEXT ? policy.canSendText() : policy.canSendTemplate();
        if (!allowed) {
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage(policy.blockedReason());
            return Optional.empty();
        }
        return Optional.of(new WhatsappGateway.DispatchCommand(message.getId(), phone, message.getKind(), message.getText()));
    }

    @Transactional
    public int complete(UUID messageId, WhatsappGateway.SendResult result) {
        return switch (result.outcome()) {
            case ACCEPTED -> messageRepository.completeDispatch(messageId, MessageStatus.ACCEPTED.name(),
                    result.providerMessageId(), null);
            case FAILED -> messageRepository.completeDispatch(messageId, MessageStatus.FAILED.name(),
                    null, result.errorMessage());
            case UNKNOWN -> messageRepository.completeDispatch(messageId, MessageStatus.UNKNOWN.name(),
                    null, result.errorMessage());
        };
    }

    @Transactional
    public int recoverStaleClaims() {
        var now = clock.instant();
        return messageRepository.markStaleSendingUnknown(now.minus(Duration.ofMinutes(2)), now);
    }
}
