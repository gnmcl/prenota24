package com.prenota24.backend.service;

import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.dto.UpdateAppUserRequest;

import java.util.UUID;

public interface IAppUserService {
    /** Crea un utente nello studio dell'admin autenticato ({@code studioId}). */
    AppUserResponse create(CreateAppUserRequest request, UUID studioId);
    AppUserResponse getById(UUID id);
    AppUserResponse update(UUID id, UUID requestingUserId, UpdateAppUserRequest request);
}
