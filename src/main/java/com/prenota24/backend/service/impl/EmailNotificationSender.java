package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.NotificationSendException;
import com.prenota24.backend.config.ResendProperties;
import com.prenota24.backend.domain.Notification;
import com.prenota24.backend.domain.NotificationChannel;
import com.prenota24.backend.domain.NotificationStatus;
import com.prenota24.backend.repository.NotificationRepository;
import com.prenota24.backend.service.NotificationSender;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * NotificationSender implementation for the EMAIL channel.
 * Replaced SMTP (JavaMailSender) with Resend HTTP API to work on
 * hosting providers that block outbound SMTP ports (e.g. Render Free tier).
 * All other architecture (NotificationDispatcher, NotificationService,
 * NotificationSender interface) is unchanged.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSender.class);

    // Resend HTTP client — thread-safe singleton provided by ResendConfig
    private final Resend resendClient;
    private final ResendProperties resendProperties;
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
            String recipientName  = (String) payload.getOrDefault("recipientName", "");
            String subject        = (String) payload.getOrDefault("subject", "Notifica da Prenota24");
            String body           = (String) payload.getOrDefault("body", "");

            if (recipientEmail == null || recipientEmail.isBlank()) {
                throw new NotificationSendException("Email destinatario mancante", null);
            }

            // If the payload carries a separate HTML field (future extension), use it;
            // otherwise fall back to plain body for both text and html content.
            String html = payload.containsKey("html")
                    ? (String) payload.get("html")
                    : body;

            // Resend "from" format: "Name <email@domain.com>"
            String from = resendProperties.fromName() + " <" + resendProperties.fromAddress() + ">";

            var options = CreateEmailOptions.builder()
                    .from(from)
                    .to(recipientEmail)
                    .subject(subject)
                    .html(html)
                    .text(body)
                    .build();

            resendClient.emails().send(options);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            logger.info("Email inviata a {} [from={}] per tipo {}",
                    recipientEmail, resendProperties.fromAddress(), notification.getType());

        } catch (NotificationSendException e) {
            markFailed(notification, e.getMessage(), null);
            throw e;
        } catch (ResendException e) {
            // Checked exception — errore HTTP/SDK con status code e body già nel getMessage()
            markFailed(notification, e.getMessage(), e);
            throw new NotificationSendException("Invio email fallito via Resend: " + e.getMessage(), e);
        } catch (Exception e) {
            // RuntimeException lanciata dal SDK per risposte HTTP 4xx/5xx (messaggio = statusCode + responseBody)
            markFailed(notification, e.getMessage(), e);
            throw new NotificationSendException("Invio email fallito: " + e.getMessage(), e);
        }
    }

    private void markFailed(Notification notification, String error, Throwable cause) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setErrorMessage(error);
        notification.setRetryCount((short) (notification.getRetryCount() + 1));
        notificationRepository.save(notification);
        // Passa la causa al logger per includere stack trace completo e responseBody nei log
        logger.error("Email fallita per notifica {} [from={}]: {}",
                notification.getId(), resendProperties.fromAddress(), error, cause);
    }
}
