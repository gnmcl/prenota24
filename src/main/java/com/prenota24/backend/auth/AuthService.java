package com.prenota24.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.config.JwtProperties;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.InvitationStatus;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.AcceptInvitationRequest;
import com.prenota24.backend.dto.AuthUserResponse;
import com.prenota24.backend.dto.LoginRequest;
import com.prenota24.backend.dto.LoginResponse;
import com.prenota24.backend.dto.RegisterRequest;
import com.prenota24.backend.dto.RegisterResponse;
import com.prenota24.backend.dto.ResendVerificationRequest;
import com.prenota24.backend.dto.VerifyEmailRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.repository.TeamInvitationRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_EXPIRATION_MINUTES = 15;

    private final AppUserRepository appUserRepository;
    private final StudioRepository studioRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecretKey key;
    private final int expirationHours;

    @Value("${spring.mail.properties.mail.from:noreply@prenota24.com}")
    private String mailFrom;

    public AuthService(AppUserRepository appUserRepository,
                       StudioRepository studioRepository,
                       TeamInvitationRepository teamInvitationRepository,
                       PasswordEncoder passwordEncoder,
                       JavaMailSender mailSender,
                       JwtProperties jwtProperties) {
        this.appUserRepository = appUserRepository;
        this.studioRepository = studioRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationHours = jwtProperties.expirationHours();
    }

    public LoginResponse login(LoginRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User is inactive");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email non verificata. Controlla la tua casella di posta.");
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
        if (appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        // Generate unique slug from studio name
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
                .emailVerified(false)
                .build();

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(Instant.now().plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
        user = appUserRepository.save(user);

        sendVerificationEmail(user.getEmail(), user.getName(), code);

        return new RegisterResponse(user.getEmail(), "Codice di verifica inviato a " + user.getEmail());
    }

    // ── Email Verification ─────────────────────────────────────────

    @Transactional
    public LoginResponse verifyEmail(VerifyEmailRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email già verificata");
        }

        if (user.getVerificationCode() == null ||
                !user.getVerificationCode().equals(request.code())) {
            throw new RuntimeException("Codice non valido");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Il codice è scaduto. Richiedi un nuovo codice.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        appUserRepository.save(user);

        var token = generateToken(user.getId().toString(), user.getRole().toString());
        var authUser = toAuthUserResponse(user);
        return new LoginResponse(token, authUser);
    }

    @Transactional
    public void resendVerificationCode(ResendVerificationRequest request) {
        var user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email già verificata");
        }

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(Instant.now().plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
        appUserRepository.save(user);

        sendVerificationEmail(user.getEmail(), user.getName(), code);
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

        // Check if email is already registered
        if (appUserRepository.findByEmail(invitation.getEmail()).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        // Create AppUser linked to the professional and studio
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

        // Mark invitation as accepted
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        var token = generateToken(user.getId().toString(), user.getRole().toString());
        var authUser = toAuthUserResponse(user);

        return new LoginResponse(token, authUser);
    }

    // ────────────────────────────────────────────────────────────────

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

    private String generateToken(String userId, String role) {
        Instant now = Instant.now();
        Instant expirationTime = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }

    // ── Studio slug generation ──────────────────────────────────────

    private String generateStudioSlug(String name) {
        String base = toSlug(name);
        if (base.isBlank()) {
            base = "studio";
        }
        String slug = base;

        // Guarantee uniqueness
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

    private void sendVerificationEmail(String email, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Prenota24 – Codice di verifica");
            message.setText(
                    "Ciao " + (name != null ? name : "") + ",\n\n" +
                    "Il tuo codice di verifica è: " + code + "\n\n" +
                    "Il codice scade tra " + CODE_EXPIRATION_MINUTES + " minuti.\n\n" +
                    "Se non hai richiesto questa registrazione, ignora questa email.\n\n" +
                    "— Prenota24"
            );
            mailSender.send(message);
            logger.info("Verification email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", email, e.getMessage());
        }
    }
}
