package com.prenota24.backend.config;

import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsappProperties(
        boolean enabled,
        UUID pilotStudioId,
        String phoneNumberId,
        String accessToken,
        String appSecret,
        String verifyToken,
        String templateName,
        String templateLanguage,
        String apiVersion,
        Duration connectTimeout,
        Duration requestTimeout
) {
    public boolean isConfiguredFor(UUID studioId) {
        return enabled
                && studioId != null
                && studioId.equals(pilotStudioId)
                && hasText(phoneNumberId)
                && hasText(accessToken)
                && hasText(appSecret)
                && hasText(verifyToken)
                && hasText(templateName)
                && hasText(templateLanguage)
                && hasText(apiVersion);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
