package com.prenota24.backend.service.impl;

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
    public AppUserResponse create(CreateAppUserRequest request) {
        appUserRepository.findByEmailAndStudioId(request.email(), request.studioId())
                .ifPresent(existing -> {
                    throw new com.prenota24.backend.common.EmailAlreadyRegisteredException(
                            "Email già registrata nello studio");
                });

        var studio = studioRepository.findById(request.studioId())
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var newUser = AppUser.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .studio(studio)
                .build();

        return toResponse(appUserRepository.save(newUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserResponse getById(UUID id) {
        var user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public AppUserResponse update(UUID id, UUID requestingUserId, UpdateAppUserRequest request) {
        var appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        // Solo l'utente stesso o un ADMIN dello stesso studio possono aggiornare
        var requestingUser = appUserRepository.findById(requestingUserId)
                .orElseThrow(() -> new EntityNotFoundException("Utente autenticato non trovato"));

        boolean isSelf = appUser.getId().equals(requestingUserId);
        boolean isAdminSameStudio = requestingUser.getRole() == com.prenota24.backend.domain.UserRole.ADMIN
                && requestingUser.getStudio().getId().equals(appUser.getStudio().getId());

        if (!isSelf && !isAdminSameStudio) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Non hai i permessi per modificare questo utente");
        }

        if (request.name() != null && !request.name().isBlank()) {
            appUser.setName(request.name());
        }
        if (request.email() != null && !request.email().isBlank()) {
            appUser.setEmail(request.email().trim().toLowerCase());
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
