package com.prenota24.backend.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.AvailabilityExceptionResponse;
import com.prenota24.backend.dto.AvailabilityResponse;
import com.prenota24.backend.dto.AvailabilitySlotRequest;
import com.prenota24.backend.dto.CreateAvailabilityExceptionRequest;
import com.prenota24.backend.dto.CreateProfessionalRequest;
import com.prenota24.backend.dto.ProfessionalResponse;
import com.prenota24.backend.dto.TimeSlotResponse;
import com.prenota24.backend.dto.UpdateProfessionalRequest;
import com.prenota24.backend.service.IProfessionalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Gestione professionisti dello studio (CRUD solo ADMIN, lettura tutti)")
public class ProfessionalController {

    private final IProfessionalService professionalService;
    private final AuthHelper authHelper;

    @GetMapping
    @Operation(summary = "Lista professionisti dello studio")
    public List<ProfessionalResponse> list(Authentication auth) {
        return professionalService.getByStudio(authHelper.getStudioId(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea professionista (solo ADMIN)")
    public ProfessionalResponse create(@RequestBody @Valid CreateProfessionalRequest request,
                                       Authentication auth) {
        return professionalService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio professionista")
    public ProfessionalResponse getById(@PathVariable UUID id, Authentication auth) {
        return professionalService.getById(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aggiorna professionista (solo ADMIN)")
    public ProfessionalResponse update(@PathVariable UUID id,
                                       @RequestBody @Valid UpdateProfessionalRequest request,
                                       Authentication auth) {
        return professionalService.update(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina professionista (solo ADMIN)")
    public void delete(@PathVariable UUID id, Authentication auth) {
        professionalService.delete(id, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Disponibilità settimanale del professionista")
    public List<AvailabilityResponse> getAvailability(@PathVariable UUID id, Authentication auth) {
        return professionalService.getAvailability(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Imposta disponibilità settimanale (rimpiazza tutte le fasce)")
    public List<AvailabilityResponse> setAvailability(@PathVariable UUID id,
                                                      @RequestBody @Valid List<AvailabilitySlotRequest> slots,
                                                      Authentication auth) {
        return professionalService.setAvailability(id, slots, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}/exceptions")
    @Operation(summary = "Eccezioni di disponibilità (chiusure / orari alternativi)")
    public List<AvailabilityExceptionResponse> getExceptions(@PathVariable UUID id, Authentication auth) {
        return professionalService.getExceptions(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aggiungi eccezione di disponibilità")
    public AvailabilityExceptionResponse addException(@PathVariable UUID id,
                                                      @RequestBody @Valid CreateAvailabilityExceptionRequest request,
                                                      Authentication auth) {
        return professionalService.addException(id, request, authHelper.getStudioId(auth));
    }

    @DeleteMapping("/{id}/exceptions/{exId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rimuovi eccezione di disponibilità")
    public void removeException(@PathVariable UUID id,
                                @PathVariable UUID exId,
                                Authentication auth) {
        professionalService.removeException(id, exId, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Slot liberi calcolati per una data")
    public List<TimeSlotResponse> getSlots(@PathVariable UUID id,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam int durationMinutes,
                                           Authentication auth) {
        return professionalService.getAvailableSlots(id, date, durationMinutes, authHelper.getStudioId(auth));
    }
}
