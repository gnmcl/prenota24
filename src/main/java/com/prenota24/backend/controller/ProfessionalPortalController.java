package com.prenota24.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.AvailabilityExceptionResponse;
import com.prenota24.backend.dto.AvailabilityResponse;
import com.prenota24.backend.dto.AvailabilitySlotRequest;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.dto.ClientSummaryResponse;
import com.prenota24.backend.dto.CreateAppointmentRequest;
import com.prenota24.backend.dto.CreateAvailabilityExceptionRequest;
import com.prenota24.backend.dto.CreateClientRequest;
import com.prenota24.backend.dto.ProfessionalDashboardResponse;
import com.prenota24.backend.dto.ServiceTypeResponse;
import com.prenota24.backend.dto.StudioResponse;
import com.prenota24.backend.service.IProfessionalPortalService;
import com.prenota24.backend.service.IStudioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('PROFESSIONAL')")
@RequiredArgsConstructor
@Tag(name = "Professional Portal", description = "Portale del professionista: dashboard, appuntamenti, clienti, disponibilità (solo PROFESSIONAL)")
public class ProfessionalPortalController {

    private final IProfessionalPortalService portalService;
    private final IStudioService studioService;
    private final AuthHelper authHelper;

    // ── Dashboard ──────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard professionista", description = "Appuntamenti di oggi, clienti totali, richieste in attesa")
    public ProfessionalDashboardResponse dashboard(Authentication auth) {
        return portalService.getDashboard(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    // ── Profile ──────────────────────────────────────

    @GetMapping("/me")
    public ProfessionalDashboardResponse me(Authentication auth) {
        return portalService.getDashboard(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    @GetMapping("/studio")
    public StudioResponse studio(Authentication auth) {
        return studioService.getById(authHelper.getStudioId(auth));
    }

    // ── Appointments ──────────────────────────────────────

    @GetMapping("/appointments")
    @Operation(summary = "Lista appuntamenti del professionista corrente")
    public Page<AppointmentResponse> listAppointments(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return portalService.getMyAppointments(authHelper.getProfessionalId(auth), status, pageable);
    }

    @GetMapping("/appointments/{id}")
    public AppointmentResponse getAppointment(@PathVariable UUID id, Authentication auth) {
        return portalService.getMyAppointmentById(id, authHelper.getProfessionalId(auth));
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@RequestBody @Valid CreateAppointmentRequest request,
                                                  Authentication auth) {
        return portalService.createAppointment(
                request,
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    @PostMapping("/appointments/{id}/confirm")
    public AppointmentResponse confirmAppointment(@PathVariable UUID id, Authentication auth) {
        return portalService.confirmAppointment(id, authHelper.getProfessionalId(auth));
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentResponse cancelAppointment(@PathVariable UUID id,
                                                  @RequestBody(required = false) CancelAppointmentRequest request,
                                                  Authentication auth) {
        return portalService.cancelAppointment(id, request, authHelper.getProfessionalId(auth));
    }

    @PostMapping("/appointments/{id}/complete")
    public AppointmentResponse completeAppointment(@PathVariable UUID id, Authentication auth) {
        return portalService.completeAppointment(id, authHelper.getProfessionalId(auth));
    }

    @PostMapping("/appointments/{id}/no-show")
    public AppointmentResponse noShowAppointment(@PathVariable UUID id, Authentication auth) {
        return portalService.noShowAppointment(id, authHelper.getProfessionalId(auth));
    }

    // ── Clients ──────────────────────────────────────

    @GetMapping("/clients")
    public List<ClientSummaryResponse> listClients(Authentication auth) {
        return portalService.getMyClients(authHelper.getProfessionalId(auth));
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientSummaryResponse createClient(@RequestBody @Valid CreateClientRequest request,
                                               Authentication auth) {
        return portalService.createClient(
                request,
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    // ── Service Types ──────────────────────────────────────

    @GetMapping("/service-types")
    public List<ServiceTypeResponse> listServiceTypes(Authentication auth) {
        return portalService.getMyServiceTypes(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    // ── Availability ──────────────────────────────────────

    @GetMapping("/availability")
    @Operation(summary = "Disponibilità settimanale del professionista corrente")
    public List<AvailabilityResponse> getAvailability(Authentication auth) {
        return portalService.getMyAvailability(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    @PutMapping("/availability")
    @Operation(summary = "Imposta disponibilità settimanale (rimpiazza tutte le fasce orarie)")
    public List<AvailabilityResponse> setAvailability(@RequestBody @Valid List<AvailabilitySlotRequest> slots,
                                                       Authentication auth) {
        return portalService.setMyAvailability(
                authHelper.getProfessionalId(auth),
                slots,
                authHelper.getStudioId(auth)
        );
    }

    @GetMapping("/exceptions")
    public List<AvailabilityExceptionResponse> getExceptions(Authentication auth) {
        return portalService.getMyExceptions(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityExceptionResponse addException(@RequestBody @Valid CreateAvailabilityExceptionRequest request,
                                                       Authentication auth) {
        return portalService.addMyException(
                authHelper.getProfessionalId(auth),
                request,
                authHelper.getStudioId(auth)
        );
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeException(@PathVariable UUID id, Authentication auth) {
        portalService.removeMyException(
                authHelper.getProfessionalId(auth),
                id,
                authHelper.getStudioId(auth)
        );
    }
}
