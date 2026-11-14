package com.prenota24.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prenota24.backend.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtProperties(SECRET, 15, 7));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredTokenDoesNotBlockTheRequest() throws Exception {
        // Il caso del refresh: il frontend allega ancora l'access token scaduto.
        var request = bearer(token(Instant.now().minus(1, ChronoUnit.MINUTES)));
        var response = new MockHttpServletResponse();
        var seenAuth = new AtomicReference<Authentication>();
        var calls = new AtomicInteger();

        filter.doFilter(request, response, (req, res) -> {
            calls.incrementAndGet();
            seenAuth.set(SecurityContextHolder.getContext().getAuthentication());
        });

        assertThat(calls).hasValue(1);
        assertThat(seenAuth.get()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void validTokenAuthenticatesTheRequest() throws Exception {
        var request = bearer(token(Instant.now().plus(5, ChronoUnit.MINUTES)));
        var seenAuth = new AtomicReference<Authentication>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> seenAuth.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(seenAuth.get()).isNotNull();
        assertThat(seenAuth.get().getName()).isEqualTo("user-1");
        assertThat(seenAuth.get().getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE)).isNull();
    }

    @Test
    void downstreamExceptionIsNotMistakenForInvalidTokenNorRunsChainTwice() {
        var request = bearer(token(Instant.now().plus(5, ChronoUnit.MINUTES)));
        var calls = new AtomicInteger();
        FilterChain failing = (req, res) -> {
            calls.incrementAndGet();
            throw new ServletException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), failing))
                .isInstanceOf(ServletException.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void entryPointReportsInvalidTokenWhenTheFilterFlaggedIt() throws Exception {
        var entryPoint = new JwtAuthenticationEntryPoint(new ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/appointments");
        request.setAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE, Boolean.TRUE);
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_TOKEN");
    }

    private static MockHttpServletRequest bearer(String token) {
        var request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static String token(Instant expiresAt) {
        return Jwts.builder()
                .subject("user-1")
                .claim("role", "ADMIN")
                .issuedAt(Date.from(expiresAt.minus(15, ChronoUnit.MINUTES)))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
