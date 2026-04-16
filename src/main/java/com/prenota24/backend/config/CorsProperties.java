package com.prenota24.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
@Validated
public record CorsProperties(
        List<@NotBlank String> allowedOrigins
) {}
