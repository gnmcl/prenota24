package com.prenota24.backend.service;

import com.prenota24.backend.dto.CreateStudioRequest;
import com.prenota24.backend.dto.EditStudioProfileRequest;
import com.prenota24.backend.dto.StudioResponse;

import java.util.UUID;

public interface IStudioService {
    StudioResponse create(CreateStudioRequest request);
    StudioResponse getById(UUID id);
    StudioResponse editStudioProfile(UUID studioId, EditStudioProfileRequest request);
}
