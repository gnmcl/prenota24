package com.prenota24.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.mail")
@Validated
public record MailProperties(
        @NotBlank String host,
        @Positive int port,
        @NotBlank String username,
        @NotBlank String password
) {}
