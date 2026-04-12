package com.prenota24.backend.service.impl;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.Availability;
import com.prenota24.backend.domain.AvailabilityException;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.repository.AvailabilityExceptionRepository;
import com.prenota24.backend.repository.AvailabilityRepository;
import com.prenota24.backend.repository.ProfessionalRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IProfessionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfessionalService implements IProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository exceptionRepository;
    private final StudioRepository studioRepository;
    private final SlotCalculatorService slotCalculatorService;

    @Override
    @Transactional
    public ProfessionalResponse create(CreateProfessionalRequest request, UUID studioId) {
        var studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new EntityNotFoundException("Studio non trovato"));

        var professional = Professional.builder()
                .studio(studio)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .active(true)
                .build();

        professional = professionalRepository.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfessionalResponse getById(UUID id, UUID studioId) {
        return toResponse(findByIdAndStudio(id, studioId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> getByStudio(UUID studioId) {
        return professionalRepository.findByStudioIdOrderByLastNameAsc(studioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProfessionalResponse update(UUID id, UpdateProfessionalRequest request, UUID studioId) {
        var professional = findByIdAndStudio(id, studioId);

        if (request.firstName() != null) professional.setFirstName(request.firstName());
        if (request.lastName() != null) professional.setLastName(request.lastName());
        if (request.email() != null) professional.setEmail(request.email());
        if (request.phone() != null) professional.setPhone(request.phone());
        if (request.active() != null) professional.setActive(request.active());

        professional = professionalRepository.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID studioId) {
        var professional = findByIdAndStudio(id, studioId);
        professional.setActive(false);
        professionalRepository.save(professional);
    }

    // ── Availability ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(UUID professionalId, UUID studioId) {
        findByIdAndStudio(professionalId, studioId);
        return availabilityRepository.findByProfessionalId(professionalId)
                .stream()
                .map(this::toAvailabilityResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<AvailabilityResponse> setAvailability(UUID professionalId, List<AvailabilitySlotRequest> slots, UUID studioId) {
        var professional = findByIdAndStudio(professionalId, studioId);

        availabilityRepository.deleteByProfessionalId(professionalId);

        var entities = slots.stream().map(slot -> Availability.builder()
                .professional(professional)
                .dayOfWeek(slot.dayOfWeek())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .build()
        ).toList();

        return availabilityRepository.saveAll(entities)
                .stream()
                .map(this::toAvailabilityResponse)
                .toList();
    }

    // ── Exceptions ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityExceptionResponse> getExceptions(UUID professionalId, UUID studioId) {
        findByIdAndStudio(professionalId, studioId);
        return exceptionRepository.findByProfessionalIdAndDateBetween(
                        professionalId,
                        LocalDate.now(),
                        LocalDate.now().plusMonths(6)
                ).stream()
                .map(this::toExceptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public AvailabilityExceptionResponse addException(UUID professionalId,
                                                       CreateAvailabilityExceptionRequest request,
                                                       UUID studioId) {
        var professional = findByIdAndStudio(professionalId, studioId);

        var exception = AvailabilityException.builder()
                .professional(professional)
                .date(request.date())
                .isUnavailable(request.isUnavailable())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .build();

        exception = exceptionRepository.save(exception);
        return toExceptionResponse(exception);
    }

    @Override
    @Transactional
    public void removeException(UUID professionalId, UUID exceptionId, UUID studioId) {
        findByIdAndStudio(professionalId, studioId);
        var exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new EntityNotFoundException("Eccezione non trovata"));

        if (!exception.getProfessional().getId().equals(professionalId)) {
            throw new EntityNotFoundException("Eccezione non trovata");
        }

        exceptionRepository.delete(exception);
    }

    // ── Slots ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(UUID professionalId, LocalDate date, int durationMinutes, UUID studioId) {
        var professional = findByIdAndStudio(professionalId, studioId);
        var studio = professional.getStudio();
        return slotCalculatorService.calculateSlots(professionalId, date, durationMinutes, studio);
    }

    // ── Helpers ──────────────────────────────────────

    private Professional findByIdAndStudio(UUID id, UUID studioId) {
        return professionalRepository.findByIdAndStudioId(id, studioId)
                .orElseThrow(() -> new EntityNotFoundException("Professionista non trovato"));
    }

    private ProfessionalResponse toResponse(Professional p) {
        return new ProfessionalResponse(
                p.getId(),
                p.getStudio().getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getPhone(),
                p.isActive(),
                p.getCreatedAt()
        );
    }

    private AvailabilityResponse toAvailabilityResponse(Availability a) {
        return new AvailabilityResponse(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime());
    }

    private AvailabilityExceptionResponse toExceptionResponse(AvailabilityException e) {
        return new AvailabilityExceptionResponse(
                e.getId(), e.getDate(), e.isUnavailable(), e.getStartTime(), e.getEndTime(), e.getReason()
        );
    }
}
