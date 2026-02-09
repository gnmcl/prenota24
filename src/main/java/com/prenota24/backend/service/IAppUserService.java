package com.prenota24.backend.service;

import com.prenota24.backend.dto.AppUserResponse;
import com.prenota24.backend.dto.CreateAppUserRequest;

import java.util.UUID;

public interface IAppUserService {
    AppUserResponse create(CreateAppUserRequest request);
    AppUserResponse getById(UUID id);
}
