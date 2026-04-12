package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationStatus;
import com.prenota24.backend.repository.NotificationRepository;
import com.prenota24.backend.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationDispatcher(NotificationRepository notificationRepository,
                                   List<NotificationSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    @Async
    public void dispatch(Notification notification) {
        var sender = senders.get(notification.getChannel());
        if (sender == null) {
            logger.warn("Nessun sender trovato per canale {}, notifica {} skipped",
                    notification.getChannel(), notification.getId());
            notification.setStatus(NotificationStatus.SKIPPED);
            notificationRepository.save(notification);
            return;
        }

        try {
            sender.send(notification);
        } catch (Exception e) {
            logger.error("Dispatch fallito per notifica {}: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * Process all pending notifications that are ready to be sent.
     */
    public void processPending() {
        var pending = notificationRepository.findPendingToSend(Instant.now());
        for (var notification : pending) {
            dispatch(notification);
        }
    }
}
