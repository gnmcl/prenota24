package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.NotificationSendException;
import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationStatus;
import com.prenota24.backend.repository.NotificationRepository;
import com.prenota24.backend.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        try {
            Map<String, Object> payload = notification.getPayload();
            if (payload == null) {
                throw new NotificationSendException("Payload vuoto", null);
            }

            String recipientEmail = (String) payload.get("recipientEmail");
            String recipientName = (String) payload.getOrDefault("recipientName", "");
            String subject = (String) payload.getOrDefault("subject", "Notifica da Prenota24");
            String body = (String) payload.getOrDefault("body", "");

            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new NotificationSendException("Email destinatario mancante", null);
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            logger.info("Email inviata a {} per tipo {}", recipientEmail, notification.getType());

        } catch (NotificationSendException e) {
            markFailed(notification, e.getMessage());
            throw e;
        } catch (Exception e) {
            markFailed(notification, e.getMessage());
            throw new NotificationSendException("Invio email fallito: " + e.getMessage(), e);
        }
    }

    private void markFailed(Notification notification, String error) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(error);
        notification.setRetryCount((short) (notification.getRetryCount() + 1));
        notificationRepository.save(notification);
        logger.error("Email fallita per notifica {}: {}", notification.getId(), error);
    }
}
