package com.prenota24.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EmailAlreadyRegisteredException;
import com.prenota24.backend.common.EmailNotVerifiedException;
import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.InvalidPasswordResetCodeException;
import com.prenota24.backend.config.JwtProperties;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.InvitationStatus;
import com.prenota24.backend.domain.RefreshToken;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.AcceptInvitationRequest;
import com.prenota24.backend.dto.AuthUserResponse;
import com.prenota24.backend.dto.ChangeEmailRequest;
import com.prenota24.backend.dto.ChangePasswordRequest;
import com.prenota24.backend.dto.LoginRequest;
import com.prenota24.backend.dto.LoginResponse;
import com.prenota24.backend.dto.RecoverPasswordRequest;
import com.prenota24.backend.dto.RefreshTokenRequest;
import com.prenota24.backend.dto.RegisterRequest;
import com.prenota24.backend.dto.RegisterResponse;
import com.prenota24.backend.dto.ResendVerificationRequest;
import com.prenota24.backend.dto.ResetPasswordRequest;
import com.prenota24.backend.dto.VerifyEmailRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.RefreshTokenRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.repository.TeamInvitationRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final int PASSWORD_RESET_RESEND_COOLDOWN_SECONDS = 60;

    private final AppUserRepository appUserRepository;
    private final StudioRepository studioRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecretKey key;
    private final int accessTokenMinutes;
    private final int refreshTokenDays;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public AuthService(AppUserRepository appUserRepository,
                       StudioRepository studioRepository,
                       TeamInvitationRepository teamInvitationRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender,
                       JwtProperties jwtProperties) {
        this.appUserRepository = appUserRepository;
        this.studioRepository = studioRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = jwtProperties.accessTokenMinutes();
        this.refreshTokenDays = jwtProperties.refreshTokenDays();
    }

    // ── Login ──────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email non verificata. Controlla la tua casella di posta.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        logger.info("User logged in: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    // ── Register ───────────────────────────────────────────────────

    private static final int UNVERIFIED_EXPIRY_MINUTES = 30;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        Optional<AppUser> existingUser = appUserRepository.findByEmail(request.email());

        if (existingUser.isPresent()) {
            AppUser existing = existingUser.get();

            if (existing.isEmailVerified()) {
                throw new EmailAlreadyRegisteredException("Email già registrata");
            }

            Instant expiryThreshold = Instant.now().minus(UNVERIFIED_EXPIRY_MINUTES, ChronoUnit.MINUTES);

            if (existing.getCreatedAt().isAfter(expiryThreshold)) {
                // Within 30 min: update data and resend verification code
                existing.setName(request.name());
                existing.setPasswordHash(passwordEncoder.encode(request.password()));
                existing.getStudio().setName(request.studioName());

                sendVerificationEmail(existing.getEmail(), existing);

                logger.info("Registration retry: new code sent to {}", existing.getEmail());
                return new RegisterResponse(existing.getEmail(),
                        "Codice di verifica reinviato. Controlla la tua email.");
            }

            // Expired (>30 min): delete stale unverified user and studio, then re-register
            Studio oldStudio = existing.getStudio();
            appUserRepository.delete(existing);
            studioRepository.delete(oldStudio);
            appUserRepository.flush();
            logger.info("Expired unverified user removed: {}", request.email());
        }

        // Fresh registration
        var studioSlug = generateStudioSlug(request.studioName());

        var studio = Studio.builder()
                .name(request.studioName())
                .slug(studioSlug)
                .build();
        studio = studioRepository.save(studio);

        var user = AppUser.builder()
                .studio(studio)
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        user = appUserRepository.save(user);
        sendVerificationEmail(user.getEmail(), user);

        logger.info("New studio registered: {} ({})", studio.getName(), user.getEmail());
        return new RegisterResponse(user.getEmail(), "Registrazione completata. Controlla la tua email per il codice di verifica.");
    }

    private @NonNull String getVerificationCodeAndSetCodeExpiration(AppUser existing) {
        String code = generateVerificationCode();
        existing.setVerificationCode(code);
        existing.setVerificationCodeExpiresAt(Instant.now().plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
        appUserRepository.save(existing);
        return code;
    }

    // ── Email verification ─────────────────────────────────────────

    @Transactional
    public LoginResponse verifyEmail(VerifyEmailRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email già verificata");
        }

        if (user.getVerificationCode() == null ||
            !user.getVerificationCode().equals(request.code())) {
            throw new RuntimeException("Codice non valido");
        }

        if (user.getVerificationCodeExpiresAt() != null &&
            user.getVerificationCodeExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Codice scaduto. Richiedi un nuovo codice.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        appUserRepository.save(user);

        logger.info("Email verified for user: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    @Transactional
    public void resendVerificationCode(ResendVerificationRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email già verificata");
        }

        getVerificationCodeAndSetCodeExpiration(user);
        sendVerificationEmail(user.getEmail(), user);

        logger.info("Verification code resent to: {}", user.getEmail());
    }

    // ── Accept Invitation ──────────────────────────────────────────

    @Transactional
    public LoginResponse acceptInvitation(AcceptInvitationRequest request) {
        var invitation = teamInvitationRepository.findByToken(request.token())
                .orElseThrow(() -> new RuntimeException("Invito non trovato"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Questo invito è già stato utilizzato o revocato");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(invitation);
            throw new RuntimeException("Questo invito è scaduto");
        }

        if (appUserRepository.findByEmail(invitation.getEmail()).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        var user = AppUser.builder()
                .studio(invitation.getStudio())
                .email(invitation.getEmail())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.PROFESSIONAL)
                .professional(invitation.getProfessional())
                .active(true)
                .emailVerified(true)
                .build();
        user = appUserRepository.save(user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        logger.info("Invitation accepted by: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    // ── Refresh token rotation ─────────────────────────────────────

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256(request.refreshToken());

        var storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            // Possible token reuse attack: revoke ALL tokens for this user
            refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId());
            logger.warn("Refresh token reuse detected for user {}", storedToken.getUser().getEmail());
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (storedToken.isExpired()) {
            throw new BadCredentialsException("Refresh token expired");
        }

        // Rotate: revoke old, issue new
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        var user = storedToken.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return buildLoginResponse(user);
    }

    // ── Logout (revoke all refresh tokens) ─────────────────────────

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("All refresh tokens revoked for user {}", userId);
    }

    // ── Account settings ───────────────────────────────────────────

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        var user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Password attuale non corretta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        // Revoke all refresh tokens to force re-login on other devices
        refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("Password changed for user {}", user.getEmail());
    }

    @Transactional
    public AuthUserResponse changeEmail(UUID userId, ChangeEmailRequest request) {
        var user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Password non corretta");
        }

        String normalizedEmail = request.newEmail().trim().toLowerCase();

        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email già in uso");
        }

        user.setEmail(normalizedEmail);
        appUserRepository.save(user);
        logger.info("Email changed for user {}: new email {}", userId, normalizedEmail);
        return toAuthUserResponse(user);
    }

    // ── Internal helpers ───────────────────────────────────────────

    private LoginResponse buildLoginResponse(AppUser user) {
        var accessToken = generateAccessToken(user.getId().toString(), user.getRole().toString());
        var refreshTokenValue = generateRefreshTokenValue();
        storeRefreshToken(user, refreshTokenValue);
        var authUser = toAuthUserResponse(user);
        return new LoginResponse(accessToken, refreshTokenValue, authUser);
    }

    private AuthUserResponse toAuthUserResponse(AppUser user) {
        UUID professionalId = user.getProfessional() != null
                ? user.getProfessional().getId()
                : null;

        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().toString(),
                user.getStudio().getId(),
                professionalId
        );
    }

    private String generateAccessToken(String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void storeRefreshToken(AppUser user, String rawToken) {
        var entity = RefreshToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .expiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(entity);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ── Studio slug generation ──────────────────────────────────────

    private String generateStudioSlug(String name) {
        String base = toSlug(name);
        if (base.isBlank()) {
            base = "studio";
        }
        String slug = base;
        int counter = 0;
        while (studioRepository.findBySlug(slug).isPresent()) {
            counter++;
            slug = base + "-" + counter;
        }
        return slug;
    }

    private static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        return withoutAccents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    // ── Verification helpers ────────────────────────────────────────

    private String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendVerificationEmail(String email, AppUser existingUser) {
        try {
            var name  = existingUser.getName();
            var code  = getVerificationCodeAndSetCodeExpiration(existingUser);
            var text =
                    "Ciao " + (name != null ? name : "") + ",\n\n" +
                            "Il tuo codice di verifica è: " + code + "\n\n" +
                            "Il codice scade tra " + CODE_EXPIRATION_MINUTES + " minuti.\n\n" +
                            "Se non hai richiesto questa registrazione, ignora questa email.\n\n" +
                            "— Prenota24";
            
            var message = getSimpleMailMessage(email, "Prenota24 – Codice di verifica", text);
            mailSender.send(message);
            logger.info("Verification email sent to {}", email);
        } catch (MailException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    // ── Password recovery ───────────────────────────────────────────────────

    /**
     * Invia un codice di reset password via email.
     * Risponde sempre con successo per non rivelare se la mail esiste nel sistema (anti-enumeration).
     */
    @Transactional
    public void recoverPassword(RecoverPasswordRequest request) {
        var optUser = appUserRepository.findByEmail(request.email().trim().toLowerCase());
        if (optUser.isEmpty()) {
            // Anti-enumeration: non rivelare se l'email esiste o meno
            return;
        }

        Instant now = Instant.now();
        var user = optUser.get();

        if (isPasswordRecoveryRateLimited(user, now)) {
            logger.warn("Password recovery throttled for {}", user.getEmail());
            return;
        }

        boolean hasActiveCode = hasActivePasswordResetCode(user, now);
        boolean isNewRecoveryCycle = !hasActiveCode;
        boolean consumeImmediateResend = hasActiveCode && !user.isPasswordResetImmediateResendUsed();

        sendPasswordResetEmail(user, now, isNewRecoveryCycle, consumeImmediateResend);
        logger.info("Password reset code sent to {}", user.getEmail());
    }

    /**
     * Verifica il codice di reset e imposta la nuova password.
     * Non richiede la vecchia password perché l'utente l'ha dimenticata.
     * Revoca tutti i refresh token dopo il reset per forzare il re-login sugli altri dispositivi.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var user = appUserRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new InvalidPasswordResetCodeException("Codice non valido o scaduto"));

        if (user.getPasswordResetCode() == null
                || !user.getPasswordResetCode().equals(request.code())) {
            throw new InvalidPasswordResetCodeException("Codice non valido o scaduto");
        }

        if (user.getPasswordResetCodeExpiresAt() == null
                || user.getPasswordResetCodeExpiresAt().isBefore(Instant.now())) {
            // Pulisci il codice scaduto
            user.setPasswordResetCode(null);
            user.setPasswordResetCodeExpiresAt(null);
            appUserRepository.save(user);
            throw new InvalidPasswordResetCodeException("Codice non valido o scaduto");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetCode(null);
        user.setPasswordResetCodeExpiresAt(null);
        appUserRepository.save(user);

        // Revoca tutti i refresh token: qualunque sessione aperta deve ri-autenticarsi
        refreshTokenRepository.revokeAllByUserId(user.getId());
        logger.info("Password reset completed for user {}", user.getEmail());
    }

    private void sendPasswordResetEmail(AppUser user, Instant now, boolean isNewRecoveryCycle, boolean consumeImmediateResend) {
        try {
            String code = generateVerificationCode();
            user.setPasswordResetCode(code);
            user.setPasswordResetCodeExpiresAt(now.plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
            user.setPasswordResetLastSentAt(now);
            if (isNewRecoveryCycle) {
                user.setPasswordResetImmediateResendUsed(false);
            } else if (consumeImmediateResend) {
                user.setPasswordResetImmediateResendUsed(true);
            }
            appUserRepository.save(user);

            var text =
                    "Ciao " + (user.getName() != null ? user.getName() : "") + ",\n\n" +
                    "Hai richiesto il recupero della tua password su Prenota24.\n\n" +
                    "Il tuo codice di recupero è: " + code + "\n\n" +
                    "Il codice scade tra " + CODE_EXPIRATION_MINUTES + " minuti.\n\n" +
                    "Se non hai richiesto il recupero della password, ignora questa email: " +
                    "il tuo account è al sicuro.\n\n" +
                    "— Prenota24";

            var message = getSimpleMailMessage(user.getEmail(), "Prenota24 – Recupero password", text);
            mailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private boolean hasActivePasswordResetCode(AppUser user, Instant now) {
        return user.getPasswordResetCode() != null
                && user.getPasswordResetCodeExpiresAt() != null
                && user.getPasswordResetCodeExpiresAt().isAfter(now);
    }

    private boolean isPasswordRecoveryRateLimited(AppUser user, Instant now) {
        Instant lastSentAt = user.getPasswordResetLastSentAt();
        if (lastSentAt == null) {
            return false;
        }

        if (!user.isPasswordResetImmediateResendUsed()) {
            return false;
        }

        return lastSentAt.plus(PASSWORD_RESET_RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS).isAfter(now);
    }

    private @NonNull SimpleMailMessage getSimpleMailMessage(String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }
}
