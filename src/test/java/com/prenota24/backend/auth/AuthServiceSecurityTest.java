package com.prenota24.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.prenota24.backend.common.InvalidPasswordResetCodeException;
import com.prenota24.backend.common.InvalidVerificationCodeException;
import com.prenota24.backend.config.JwtProperties;
import com.prenota24.backend.config.ResendProperties;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.RegisterRequest;
import com.prenota24.backend.dto.ResetPasswordRequest;
import com.prenota24.backend.dto.VerifyEmailRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.RefreshTokenRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.repository.TeamInvitationRepository;
import com.resend.Resend;

class AuthServiceSecurityTest {

    private static final String EMAIL = "owner@example.com";

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AuthService authService = new AuthService(
            appUserRepository,
            mock(StudioRepository.class),
            mock(TeamInvitationRepository.class),
            mock(RefreshTokenRepository.class),
            passwordEncoder,
            mock(Resend.class, RETURNS_DEEP_STUBS),
            new ResendProperties("re_test", "noreply@example.com", "Prenota24"),
            new JwtProperties("test-secret-test-secret-test-secret-123456", 15, 7));

    @Test
    void registerRetryDoesNotOverwriteCredentialsOfUnverifiedAccount() {
        var user = unverifiedUser("original-password");
        var originalHash = user.getPasswordHash();
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("Attacker", "Evil Studio", EMAIL, "attacker-password"));

        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        assertThat(passwordEncoder.matches("attacker-password", user.getPasswordHash())).isFalse();
        assertThat(user.getName()).isEqualTo("Owner");
        assertThat(user.getStudio().getName()).isEqualTo("Owner Studio");
    }

    @Test
    void verificationCodeIsInvalidatedAfterTooManyWrongAttempts() {
        var user = unverifiedUser("password123");
        user.setVerificationCode("123456");
        user.setVerificationCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        for (int i = 1; i < AuthService.MAX_OTP_FAILED_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(EMAIL, "000000")))
                    .isInstanceOf(InvalidVerificationCodeException.class)
                    .hasMessage("Codice non valido");
        }
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(EMAIL, "000000")))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("Troppi tentativi");

        // Anche il codice giusto non funziona più: ne serve uno nuovo.
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(EMAIL, "123456")))
                .isInstanceOf(InvalidVerificationCodeException.class);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationCode()).isNull();
    }

    @Test
    void verifyEmailForUnknownUserLooksLikeWrongCode() {
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(EMAIL, "123456")))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessage("Codice non valido");
    }

    @Test
    void passwordResetCodeIsInvalidatedAfterTooManyWrongAttempts() {
        var user = unverifiedUser("password123");
        user.setEmailVerified(true);
        user.setPasswordResetCode("654321");
        user.setPasswordResetCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        var originalHash = user.getPasswordHash();
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        for (int i = 0; i < AuthService.MAX_OTP_FAILED_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.resetPassword(
                    new ResetPasswordRequest(EMAIL, "000000", "new-password-1")))
                    .isInstanceOf(InvalidPasswordResetCodeException.class);
        }

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(EMAIL, "654321", "new-password-1")))
                .isInstanceOf(InvalidPasswordResetCodeException.class);
        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
        assertThat(user.getPasswordResetCode()).isNull();
    }

    @Test
    void correctPasswordResetCodeStillWorksAfterAFewMistakes() {
        var user = unverifiedUser("password123");
        user.setEmailVerified(true);
        user.setPasswordResetCode("654321");
        user.setPasswordResetCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(EMAIL, "000000", "new-password-1")))
                .isInstanceOf(InvalidPasswordResetCodeException.class);
        authService.resetPassword(new ResetPasswordRequest(EMAIL, "654321", "new-password-1"));

        assertThat(passwordEncoder.matches("new-password-1", user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordResetFailedAttempts()).isZero();
    }

    private AppUser unverifiedUser(String rawPassword) {
        var studio = Studio.builder().id(UUID.randomUUID()).name("Owner Studio").slug("owner-studio").build();
        var user = AppUser.builder()
                .id(UUID.randomUUID())
                .studio(studio)
                .email(EMAIL)
                .name("Owner")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.ADMIN)
                .active(true)
                .build();
        user.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        return user;
    }
}
