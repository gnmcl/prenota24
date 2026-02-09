package com.prenota24.backend.service.impl;

import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.CreateStudioRequest;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudioService implements IStudioService {

    private final StudioRepository studioRepository;

    public StudioResponse create(CreateStudioRequest request) {
        var studio = Studio.builder().name(request.name()).email(request.email()).phone(request.phone()).build();
        var savedStudio = studioRepository.save(studio);
        return toResponse(savedStudio);
    }

    public StudioResponse getById(UUID id) {
        var studio = studioRepository.findById(id).orElseThrow(() -> new IllegalStateException("Studio not found"));
        return toResponse(studio);
    }

    private StudioResponse toResponse(Studio studio) {
        return new StudioResponse(studio.getId(), studio.getName(), studio.getEmail(), studio.getPhone(), studio.getTimezone());
    }
}
