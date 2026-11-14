package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.common.JwtAuthenticationException;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.UpdateAppUserRequest;
import com.prenota24.backend.dto.UpdateProfessionalRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.AvailabilityExceptionRepository;
import com.prenota24.backend.repository.AvailabilityRepository;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.RefreshTokenRepository;
import com.prenota24.backend.repository.StudioRepository;

class AccountLifecycleTest {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final ProfessionalRepository professionalRepository = mock(ProfessionalRepository.class);

    private final AppUserService appUserService = new AppUserService(
            appUserRepository, mock(StudioRepository.class), new BCryptPasswordEncoder(4));
    private final ProfessionalService professionalService = new ProfessionalService(
            professionalRepository,
            mock(AvailabilityRepository.class),
            mock(AvailabilityExceptionRepository.class),
            mock(StudioRepository.class),
            mock(SlotCalculatorService.class),
            appUserRepository,
            refreshTokenRepository);

    private final Studio studio = Studio.builder().id(UUID.randomUUID()).name("Mine").build();

    // ── Utenti: email e isolamento ──────────────────────────────────────

    @Test
    void userUpdateRejectsEmailChange() {
        var user = user(studio, UserRole.ADMIN);
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.update(
                user.getId(), user.getId(), new UpdateAppUserRequest("Nuovo", "attacker@example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getEmail()).isEqualTo("owner@example.com");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void userUpdateStillChangesName() {
        var user = user(studio, UserRole.PROFESSIONAL);
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        appUserService.update(user.getId(), user.getId(), new UpdateAppUserRequest("Nuovo nome", null));

        assertThat(user.getName()).isEqualTo("Nuovo nome");
    }

    @Test
    void userFromAnotherStudioIsNotVisible() {
        var other = user(Studio.builder().id(UUID.randomUUID()).build(), UserRole.ADMIN);
        when(appUserRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> appUserService.getById(other.getId(), studio.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void adminOfAnotherStudioGetsNotFoundOnUpdate() {
        var victim = user(Studio.builder().id(UUID.randomUUID()).build(), UserRole.ADMIN);
        var attacker = user(studio, UserRole.ADMIN);
        when(appUserRepository.findById(victim.getId())).thenReturn(Optional.of(victim));
        when(appUserRepository.findById(attacker.getId())).thenReturn(Optional.of(attacker));

        assertThatThrownBy(() -> appUserService.update(
                victim.getId(), attacker.getId(), new UpdateAppUserRequest("x", null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── Disattivazione professionista ──────────────────────────────────────

    @Test
    void deletingProfessionalDeactivatesItsLoginAndRevokesSessions() {
        var professional = professional(true);
        var login = user(studio, UserRole.PROFESSIONAL);
        when(professionalRepository.findByIdAndStudioId(professional.getId(), studio.getId()))
                .thenReturn(Optional.of(professional));
        when(appUserRepository.findByProfessionalId(professional.getId())).thenReturn(List.of(login));

        professionalService.delete(professional.getId(), studio.getId());

        assertThat(professional.isActive()).isFalse();
        assertThat(login.isActive()).isFalse();
        verify(refreshTokenRepository).revokeAllByUserId(login.getId());
    }

    @Test
    void reactivatingProfessionalReactivatesItsLogin() {
        var professional = professional(false);
        var login = user(studio, UserRole.PROFESSIONAL);
        login.setActive(false);
        when(professionalRepository.findByIdAndStudioId(professional.getId(), studio.getId()))
                .thenReturn(Optional.of(professional));
        when(professionalRepository.save(any(Professional.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findByProfessionalId(professional.getId())).thenReturn(List.of(login));

        professionalService.update(professional.getId(),
                new UpdateProfessionalRequest(null, null, null, null, true), studio.getId());

        assertThat(login.isActive()).isTrue();
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void deactivatedUserIsRejectedEvenWithAValidAccessToken() {
        var login = user(studio, UserRole.PROFESSIONAL);
        login.setActive(false);
        when(appUserRepository.findById(login.getId())).thenReturn(Optional.of(login));
        var auth = new UsernamePasswordAuthenticationToken(login.getId().toString(), null, List.of());

        assertThatThrownBy(() -> new AuthHelper(appUserRepository).getStudioId(auth))
                .isInstanceOf(JwtAuthenticationException.class);
    }

    private AppUser user(Studio userStudio, UserRole role) {
        return AppUser.builder()
                .id(UUID.randomUUID())
                .studio(userStudio)
                .email("owner@example.com")
                .name("Owner")
                .passwordHash("hash")
                .role(role)
                .active(true)
                .build();
    }

    private Professional professional(boolean active) {
        return Professional.builder()
                .id(UUID.randomUUID())
                .studio(studio)
                .firstName("Ada")
                .lastName("Rossi")
                .active(active)
                .build();
    }
}
