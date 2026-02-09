package com.prenota24.backend.service.impl;


import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AppUserService implements IAppUserService {

    private final AppUserRepository appUserRepository;
    private final StudioRepository studioRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, StudioRepository studioRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.studioRepository = studioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUserResponse create(CreateAppUserRequest request) {

        appUserRepository.findByEmailAndStudioId(request.email(), request.studioId())
                .ifPresent(existingUser -> {
            throw new IllegalArgumentException("User with email " + request.email() + " already exists in studio " + request.studioId());
        });

        var studio = studioRepository.findById(request.studioId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Studio with id " + request.studioId() + " not found"));

        var newUser = AppUser.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .studio(studio)
                .build();

        var savedUser = appUserRepository.save(newUser);

        return toResponse(savedUser);
    }

    public AppUserResponse getById(UUID id) {
        var user = appUserRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User with id " + id + " not found"));

        return toResponse(user);
    }

    protected AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getStudio().getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}

