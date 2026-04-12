package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.service.IProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final IProfessionalService professionalService;
    private final AuthHelper authHelper;

    @GetMapping
    public List<ProfessionalResponse> list(Authentication auth) {
        return professionalService.getByStudio(authHelper.getStudioId(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessionalResponse create(@RequestBody @Valid CreateProfessionalRequest request,
                                       Authentication auth) {
        return professionalService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}")
    public ProfessionalResponse getById(@PathVariable UUID id, Authentication auth) {
        return professionalService.getById(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessionalResponse update(@PathVariable UUID id,
                                       @RequestBody @Valid UpdateProfessionalRequest request,
                                       Authentication auth) {
        return professionalService.update(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id, Authentication auth) {
        professionalService.delete(id, authHelper.getStudioId(auth));
    }

    // ── Availability ──────────────────────────────────

    @GetMapping("/{id}/availability")
    public List<AvailabilityResponse> getAvailability(@PathVariable UUID id, Authentication auth) {
        return professionalService.getAvailability(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}/availability")
    public List<AvailabilityResponse> setAvailability(@PathVariable UUID id,
                                                       @RequestBody @Valid List<AvailabilitySlotRequest> slots,
                                                       Authentication auth) {
        return professionalService.setAvailability(id, slots, authHelper.getStudioId(auth));
    }

    // ── Exceptions ──────────────────────────────────

    @GetMapping("/{id}/exceptions")
    public List<AvailabilityExceptionResponse> getExceptions(@PathVariable UUID id, Authentication auth) {
        return professionalService.getExceptions(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityExceptionResponse addException(@PathVariable UUID id,
                                                       @RequestBody @Valid CreateAvailabilityExceptionRequest request,
                                                       Authentication auth) {
        return professionalService.addException(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}/exceptions/{exId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeException(@PathVariable UUID id,
                                @PathVariable UUID exId,
                                Authentication auth) {
        professionalService.removeException(id, exId, authHelper.getStudioId(auth));
    }

    // ── Slots ──────────────────────────────────

    @GetMapping("/{id}/slots")
    public List<TimeSlotResponse> getSlots(@PathVariable UUID id,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam int durationMinutes,
                                           Authentication auth) {
        return professionalService.getAvailableSlots(id, date, durationMinutes, authHelper.getStudioId(auth));
    }
}
