package com.prenota24.backend.service;

import com.prenota24.backend.domain.EventStatus;
import com.prenota24.backend.dto.CreateEventRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.EventSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface IEventService {

    EventResponse create(CreateEventRequest request, UUID userId);

    EventResponse getBySlug(String slug);

    List<EventSummaryResponse> getByUserId(UUID userId);

    EventResponse getById(UUID id, UUID userId);

    EventResponse updateStatus(UUID id, EventStatus status, UUID userId);

    void delete(UUID id, UUID userId);
}
