package com.prenota24.backend.auth;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupTask.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteExpired(Instant.now());
        logger.info("Expired refresh tokens purged");
    }
}
