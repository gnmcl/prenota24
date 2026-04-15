package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.CreateStudioRequest;
import com.prenota24.backend.dto.EditStudioProfileRequest;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudioService implements IStudioService {

    private final StudioRepository studioRepository;

    @Override
    public StudioResponse create(CreateStudioRequest request) {
        var studio = Studio.builder().name(request.name()).email(request.email()).phone(request.phone()).build();
        var savedStudio = studioRepository.save(studio);
        return toResponse(savedStudio);
    }

    @Override
    @Transactional
    public StudioResponse editStudioProfile(UUID studioId, EditStudioProfileRequest request) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        if (request.name() != null) studio.setName(request.name());
        if (request.email() != null) studio.setEmail(request.email());
        if (request.phone() != null) studio.setPhone(request.phone());
        if (request.timezone() != null) studio.setTimezone(request.timezone());

        studio = studioRepository.save(studio);
        return toResponse(studio);
    }

    @Override
    public StudioResponse getById(UUID id) {
        var studio = studioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));
        return toResponse(studio);
    }

    private StudioResponse toResponse(Studio studio) {
        return new StudioResponse(studio.getId(), studio.getName(), studio.getEmail(), studio.getPhone(), studio.getTimezone());
    }
}
