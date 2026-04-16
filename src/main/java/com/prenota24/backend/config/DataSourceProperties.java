package com.prenota24.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public record DataSourceProperties(
        @NotBlank String url,
        @NotBlank String username,
        @NotBlank String password
) {}
