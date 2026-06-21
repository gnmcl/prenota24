package com.prenota24.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding for Resend API configuration.
 * Values are read from environment variables:
 *   RESEND_API_KEY       → resend.api-key
 *   RESEND_FROM_ADDRESS  → resend.from-address
 *   RESEND_FROM_NAME     → resend.from-name
 *
 * Auto-registered via @ConfigurationPropertiesScan in Prenota24BackendApplication.
 */
@ConfigurationProperties(prefix = "resend")
@Validated
public record ResendProperties(
        @NotBlank String apiKey,
        @NotBlank String fromAddress,
        @NotBlank String fromName
) {}

