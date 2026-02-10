package com.prenota24.backend.auth;

import com.prenota24.backend.dto.LoginRequest;
import com.prenota24.backend.dto.LoginResponse;
import com.prenota24.backend.repository.AppUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey key = Keys.hmacShaKeyFor("super-secret-prenota24-key-change-me".getBytes(StandardCharsets.UTF_8));

    public LoginResponse login(LoginRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        var token = generateToken(user.getId().toString(), user.getRole().toString());

        return new LoginResponse(token, user.getEmail(), user.getRole().toString());
    }

    private String generateToken(String userId, String role) {
        Instant now = Instant.now();
        Instant expirationTime = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }
}
