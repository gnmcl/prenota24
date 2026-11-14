package com.prenota24.backend.service;

import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.dto.UpdateAppUserRequest;

import java.util.UUID;

public interface IAppUserService {
    /** Crea un utente nello studio dell'admin autenticato ({@code studioId}). */
    AppUserResponse create(CreateAppUserRequest request, UUID studioId);
    /** Restituisce l'utente solo se appartiene allo studio {@code studioId}. */
    AppUserResponse getById(UUID id, UUID studioId);
    AppUserResponse update(UUID id, UUID requestingUserId, UpdateAppUserRequest request);
}
