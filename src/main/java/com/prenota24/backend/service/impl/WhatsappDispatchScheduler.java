package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.MessageStatus;
import com.prenota24.backend.messaging.WhatsappGateway;
import com.prenota24.backend.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsappDispatchScheduler {
    private static final Logger logger = LoggerFactory.getLogger(WhatsappDispatchScheduler.class);
    private final ConversationMessageRepository messageRepository;
    private final MessageDispatchService dispatchService;
    private final WhatsappGateway gateway;

    @Scheduled(fixedDelayString = "${whatsapp.dispatch-delay-ms:2000}")
    public void dispatchQueued() {
        dispatchService.recoverStaleClaims();
        for (var message : messageRepository.findTop50ByStatusOrderByCreatedAtAsc(MessageStatus.QUEUED)) {
            dispatchService.claim(message.getId()).ifPresent(command -> {
                WhatsappGateway.SendResult result;
                try {
                    result = gateway.send(command);
                } catch (RuntimeException ex) {
                    logger.error("Unexpected WhatsApp gateway failure for message {}", message.getId(), ex);
                    result = WhatsappGateway.SendResult.unknown("Errore inatteso dopo la presa in carico");
                }
                dispatchService.complete(message.getId(), result);
            });
        }
    }
}
