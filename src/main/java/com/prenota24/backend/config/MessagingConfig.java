package com.prenota24.backend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    @Bean
    Clock messagingClock() { return Clock.systemUTC(); }
}
