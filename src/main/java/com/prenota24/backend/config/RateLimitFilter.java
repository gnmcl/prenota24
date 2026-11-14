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
import org.springframework.scheduling.annotation.Scheduled;
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
    private static final int VERIFICATION_EMAIL_MAX_ATTEMPTS = 3;
    private static final Duration VERIFICATION_EMAIL_WINDOW = Duration.ofMinutes(1);
    private static final int PASSWORD_RECOVER_MAX_ATTEMPTS = 3;
    private static final Duration PASSWORD_RECOVER_WINDOW = Duration.ofMinutes(1);
    // Ogni richiesta pubblica occupa uno slot finché lo studio non risponde: limiti stretti
    // per IP, sia a breve termine sia giornalieri (in aggiunta al limite per email nel service).
    private static final int PUBLIC_BOOKING_MAX_ATTEMPTS = 5;
    private static final Duration PUBLIC_BOOKING_WINDOW = Duration.ofMinutes(10);
    private static final int PUBLIC_BOOKING_DAILY_MAX_ATTEMPTS = 15;
    private static final Duration PUBLIC_BOOKING_DAILY_WINDOW = Duration.ofDays(1);
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String RESEND_VERIFICATION_PATH = "/api/auth/resend-verification";
    private static final String PASSWORD_RECOVER_PATH = "/api/auth/password-recover";
    /** Lunghezza massima di un indirizzo IPv6 testuale: evita chiavi enormi nelle mappe. */
    private static final int MAX_IP_LENGTH = 45;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> verificationEmailBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> passwordRecoverBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> publicBookingBuckets = new ConcurrentHashMap<>();
    // Mappa separata: con due limiti nello stesso bucket, l'eviction dei bucket "pieni"
    // (basata sul limite più piccolo) azzererebbe anche il conteggio giornaliero.
    private final Map<String, Bucket> publicBookingDailyBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/") && !isPublicBookingCreation(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolveClientIp(request);
        if (isPublicBookingCreation(request)) {
            Bucket bookingBucket = publicBookingBuckets.computeIfAbsent(ip,
                    k -> createBucket(PUBLIC_BOOKING_MAX_ATTEMPTS, PUBLIC_BOOKING_WINDOW));
            Bucket dailyBucket = publicBookingDailyBuckets.computeIfAbsent(ip,
                    k -> createBucket(PUBLIC_BOOKING_DAILY_MAX_ATTEMPTS, PUBLIC_BOOKING_DAILY_WINDOW));
            if (!bookingBucket.tryConsume(1) || !dailyBucket.tryConsume(1)) {
                writeRateLimitedResponse(request, response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

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

        if (REGISTER_PATH.equals(request.getRequestURI()) || RESEND_VERIFICATION_PATH.equals(request.getRequestURI())) {
            Bucket verificationBucket = verificationEmailBuckets.computeIfAbsent(ip, k -> createVerificationEmailBucket());
            if (!verificationBucket.tryConsume(1)) {
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

    private Bucket createVerificationEmailBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(VERIFICATION_EMAIL_MAX_ATTEMPTS)
                .refillIntervally(VERIFICATION_EMAIL_MAX_ATTEMPTS, VERIFICATION_EMAIL_WINDOW)
                .build())
            .build();
    }

    private static Bucket createBucket(int capacity, Duration window) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, window)
                        .build())
                .build();
    }

    private boolean isPublicBookingCreation(HttpServletRequest request) {
        var path = request.getRequestURI();
        return "POST".equals(request.getMethod())
                && path.startsWith("/api/public/book/")
                && path.endsWith("/appointments");
    }

    /**
     * Rimuove periodicamente i bucket tornati pieni (IP inattivi), così le mappe non crescono
     * senza limite. Un bucket pieno equivale a uno nuovo, quindi l'eviction non allenta i limiti.
     */
    @Scheduled(fixedRate = 300_000)
    public void evictIdleBuckets() {
        evictFull(buckets, AUTH_MAX_ATTEMPTS);
        evictFull(verificationEmailBuckets, VERIFICATION_EMAIL_MAX_ATTEMPTS);
        evictFull(passwordRecoverBuckets, PASSWORD_RECOVER_MAX_ATTEMPTS);
        evictFull(publicBookingBuckets, PUBLIC_BOOKING_MAX_ATTEMPTS);
        evictFull(publicBookingDailyBuckets, PUBLIC_BOOKING_DAILY_MAX_ATTEMPTS);
    }

    private static void evictFull(Map<String, Bucket> map, int capacity) {
        map.entrySet().removeIf(entry -> entry.getValue().getAvailableTokens() >= capacity);
    }

    /**
     * Dietro un reverse proxy (es. Render) l'IP reale del client è l'ultimo valore di
     * X-Forwarded-For, quello aggiunto dal proxy. I valori precedenti li scrive il client e
     * non sono affidabili: usare il primo permetteva di aggirare il rate limit con IP inventati.
     */
    static String resolveClientIp(String remoteAddr, String xff) {
        if (xff != null && !xff.isBlank() && isPrivateOrLoopback(remoteAddr)) {
            String[] hops = xff.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (!hop.isEmpty()) {
                    return hop.length() > MAX_IP_LENGTH ? hop.substring(0, MAX_IP_LENGTH) : hop;
                }
            }
        }
        return remoteAddr;
    }

    private String resolveClientIp(HttpServletRequest request) {
        return resolveClientIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
    }

    private static boolean isPrivateOrLoopback(String ip) {
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
