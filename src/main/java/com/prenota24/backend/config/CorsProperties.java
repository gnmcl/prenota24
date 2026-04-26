package com.prenota24.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

/**
 * CORS origins possono essere configurate come lista YAML oppure come stringa
 * CSV tramite env var (es. CORS_ALLOWED_ORIGINS=http://a.com,https://b.com).
 * Il compact constructor normalizza entrambi i formati in una List<String> pulita.
 */
@ConfigurationProperties(prefix = "app.cors")
@Validated
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins.stream()
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
