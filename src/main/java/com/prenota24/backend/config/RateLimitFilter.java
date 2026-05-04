package com.prenota24.backend.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prenota24.backend.dto.ErrorResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_MAX_ATTEMPTS = 10;
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);
    private static final int PASSWORD_RECOVER_MAX_ATTEMPTS = 3;
    private static final Duration PASSWORD_RECOVER_WINDOW = Duration.ofMinutes(1);
    private static final String PASSWORD_RECOVER_PATH = "/api/auth/password-recover";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordRecoverBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        Bucket globalBucket = buckets.computeIfAbsent(ip, k -> createAuthBucket());
        if (!globalBucket.tryConsume(1)) {
            writeRateLimitedResponse(request, response);
            return;
        }

        if (PASSWORD_RECOVER_PATH.equals(request.getRequestURI())) {
            Bucket recoverBucket = passwordRecoverBuckets.computeIfAbsent(ip, k -> createPasswordRecoverBucket());
            if (!recoverBucket.tryConsume(1)) {
                writeRateLimitedResponse(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ErrorResponse.of(429, "RATE_LIMITED", "Troppi tentativi. Riprova tra poco.", request.getRequestURI())
            ));
    }

    private Bucket createAuthBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(AUTH_MAX_ATTEMPTS)
                .refillIntervally(AUTH_MAX_ATTEMPTS, AUTH_WINDOW)
                .build())
                .build();
    }

    private Bucket createPasswordRecoverBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(PASSWORD_RECOVER_MAX_ATTEMPTS)
                .refillIntervally(PASSWORD_RECOVER_MAX_ATTEMPTS, PASSWORD_RECOVER_WINDOW)
                .build())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && isPrivateOrLoopback(remoteAddr)) {
            return xff.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private boolean isPrivateOrLoopback(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
