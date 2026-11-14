package com.prenota24.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigGuardsTest {

    // ── RateLimitFilter: risoluzione IP ──────────────────────────────────────

    @Test
    void usesLastForwardedHopBehindTrustedProxy() {
        // Il client ha inserito un IP falso in testa; il proxy ha aggiunto quello reale in coda.
        assertThat(RateLimitFilter.resolveClientIp("10.0.0.5", "1.2.3.4, 203.0.113.7"))
                .isEqualTo("203.0.113.7");
    }

    @Test
    void ignoresForwardedHeaderFromPublicPeer() {
        assertThat(RateLimitFilter.resolveClientIp("198.51.100.9", "1.2.3.4"))
                .isEqualTo("198.51.100.9");
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderIsBlank() {
        assertThat(RateLimitFilter.resolveClientIp("10.0.0.5", " , ")).isEqualTo("10.0.0.5");
    }

    // ── JwtSecretGuard ──────────────────────────────────────

    private static final String DEV_SECRET = "dev-only-secret-change-in-production-min-32-chars!!";

    @Test
    void refusesDevSecretOutsideDevProfiles() {
        var guard = new JwtSecretGuard(new JwtProperties(DEV_SECRET, 15, 7),
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        assertThatThrownBy(guard::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesDevSecretWithNoProfile() {
        var guard = new JwtSecretGuard(new JwtProperties(DEV_SECRET, 15, 7), new MockEnvironment());
        assertThatThrownBy(guard::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allowsDevSecretInDevProfile() {
        var env = new MockEnvironment();
        env.setActiveProfiles("dev");
        var guard = new JwtSecretGuard(new JwtProperties(DEV_SECRET, 15, 7), env);
        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void allowsRealSecretInProd() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var guard = new JwtSecretGuard(new JwtProperties("a-real-production-secret-of-sufficient-length", 15, 7), env);
        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }
}
