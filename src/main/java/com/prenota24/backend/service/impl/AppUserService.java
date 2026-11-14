package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EmailNormalizer;
import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.dto.UpdateAppUserRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserService implements IAppUserService {

    private final AppUserRepository appUserRepository;
    private final StudioRepository studioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AppUserResponse create(CreateAppUserRequest request, UUID studioId) {
        // Lo studio di destinazione è sempre quello dell'admin autenticato: un eventuale
        // studioId nel body viene ignorato, altrimenti un admin potrebbe creare utenti
        // (anche ADMIN) in uno studio altrui e prenderne il controllo.
        var normalizedEmail = EmailNormalizer.normalize(request.email());

        // L'email è unica a livello globale (uq_user_email), non solo nello studio.
        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(existing -> {
                    throw new com.prenota24.backend.common.EmailAlreadyRegisteredException(
                            "Email già registrata");
                });

        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var newUser = AppUser.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .studio(studio)
                .build();

        return toResponse(appUserRepository.save(newUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserResponse getById(UUID id, UUID studioId) {
        // Visibili solo gli utenti dello stesso studio: per gli altri rispondiamo 404,
        // senza rivelare che l'id esiste.
        var user = appUserRepository.findById(id)
                .filter(found -> found.getStudio().getId().equals(studioId))
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public AppUserResponse update(UUID id, UUID requestingUserId, UpdateAppUserRequest request) {
        // Il cambio email passa solo da /api/account/change-email, che chiede la password:
        // qui un token rubato (o un admin) potrebbe dirottare l'account cambiando l'email
        // e poi avviando il recupero password.
        if (request.email() != null && !request.email().isBlank()) {
            throw new IllegalArgumentException(
                    "L'email non si modifica da qui: usa /api/account/change-email");
        }

        var appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        // Solo l'utente stesso o un ADMIN dello stesso studio possono aggiornare
        var requestingUser = appUserRepository.findById(requestingUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utente autenticato non trovato"));

        boolean isSelf = appUser.getId().equals(requestingUserId);
        boolean isAdminSameStudio = requestingUser.getRole() == com.prenota24.backend.domain.UserRole.ADMIN
                && requestingUser.getStudio().getId().equals(appUser.getStudio().getId());

        if (!isSelf && !isAdminSameStudio) {
            // 404 come in getById: un utente di un altro studio non deve risultare esistente.
            if (!requestingUser.getStudio().getId().equals(appUser.getStudio().getId())) {
                throw new EntityNotFoundException("Utente non trovato");
            }
            throw new org.springframework.security.access.AccessDeniedException(
                    "Non hai i permessi per modificare questo utente");
        }

        if (request.name() != null && !request.name().isBlank()) {
            appUser.setName(request.name());
        }

        return toResponse(appUserRepository.save(appUser));
    }

    protected AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getStudio().getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive());
    }
}
