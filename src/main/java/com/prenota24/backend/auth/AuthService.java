package com.prenota24.backend.auth;

import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.StudioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final StudioRepository studioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey key = Keys.hmacShaKeyFor("super-secret-prenota24-key-change-me".getBytes(StandardCharsets.UTF_8));

    public LoginResponse login(LoginRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        var token = generateToken(user.getId().toString(), user.getRole().toString());
        var authUser = toAuthUserResponse(user);

        return new LoginResponse(token, authUser);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if email already exists
        if (appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        // 1. Create Studio automatically with user's name
        var studio = Studio.builder()
                .name(request.name())
                .build();
        studio = studioRepository.save(studio);

        // 2. Create AppUser linked to the studio
        var user = AppUser.builder()
                .studio(studio)
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        user = appUserRepository.save(user);

        // 3. Generate JWT
        var token = generateToken(user.getId().toString(), user.getRole().toString());
        var authUser = toAuthUserResponse(user);

        return new RegisterResponse(token, authUser);
    }

    private AuthUserResponse toAuthUserResponse(AppUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().toString(),
                user.getStudio().getId()
        );
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
