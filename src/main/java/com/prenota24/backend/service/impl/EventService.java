package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Event;
import com.prenota24.backend.domain.EventStatus;
import com.prenota24.backend.domain.ReservationStatus;
import com.prenota24.backend.dto.CreateEventRequest;
import com.prenota24.backend.dto.EventResponse;
import com.prenota24.backend.dto.EventSummaryResponse;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.EventRepository;
import com.prenota24.backend.repository.ReservationRepository;
import com.prenota24.backend.service.IEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventService implements IEventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request, UUID userId) {
        var user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        var slug = generateSlug(request.title());

        var event = Event.builder()
                .studio(user.getStudio())
                .createdBy(user)
                .title(request.title())
                .description(request.description())
                .slug(slug)
                .eventDate(request.eventDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .maxParticipants(request.maxParticipants())
                .status(EventStatus.DRAFT)
                .build();

        event = eventRepository.save(event);
        return toEventResponse(event, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getBySlug(String slug) {
        var event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));
        var count = reservationRepository.countByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED);
        return toEventResponse(event, count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> getByUserId(UUID userId) {
        var events = eventRepository.findByCreatedByIdOrderByEventDateDesc(userId);
        return events.stream().map(event -> {
            var count = reservationRepository.countByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED);
            return toEventSummaryResponse(event, count);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(UUID id, UUID userId) {
        var event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));

        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new EntityNotFoundException("Evento non trovato");
        }

        var count = reservationRepository.countByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED);
        return toEventResponse(event, count);
    }

    @Override
    @Transactional
    public EventResponse updateStatus(UUID id, EventStatus status, UUID userId) {
        var event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));

        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new EntityNotFoundException("Evento non trovato");
        }

        event.setStatus(status);
        event = eventRepository.save(event);

        var count = reservationRepository.countByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED);
        return toEventResponse(event, count);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        var event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento non trovato"));

        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new EntityNotFoundException("Evento non trovato");
        }

        reservationRepository.deleteAllByEventId(id);
        eventRepository.delete(event);
    }

    // ── Slug generation ──────────────────────────────────────

    private String generateSlug(String title) {
        String base = toSlug(title);
        String suffix = UUID.randomUUID().toString().substring(0, 7);
        String slug = base + "-" + suffix;

        while (eventRepository.findBySlug(slug).isPresent()) {
            suffix = UUID.randomUUID().toString().substring(0, 7);
            slug = base + "-" + suffix;
        }
        return slug;
    }

    private static String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        return withoutAccents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    // ── Mapping ──────────────────────────────────────────────

    private EventResponse toEventResponse(Event event, long currentParticipants) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getSlug(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getMaxParticipants(),
                currentParticipants,
                event.getStatus().toString(),
                "/e/" + event.getSlug(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private EventSummaryResponse toEventSummaryResponse(Event event, long currentParticipants) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getSlug(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getMaxParticipants(),
                currentParticipants,
                event.getStatus().toString()
        );
    }
}
