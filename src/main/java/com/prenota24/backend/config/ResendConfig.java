package com.prenota24.backend.config;

import com.resend.Resend;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a singleton Resend HTTP client bean, built from ResendProperties.
 * The client is thread-safe and reused across all notification sends.
 */
@Configuration
@RequiredArgsConstructor
public class ResendConfig {

    private final ResendProperties resendProperties;

    @Bean
    public Resend resendClient() {
        return new Resend(resendProperties.apiKey());
    }
}

