package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.service.IProfessionalPortalService;
import com.prenota24.backend.service.IStudioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('PROFESSIONAL')")
@RequiredArgsConstructor
public class ProfessionalPortalController {

    private final IProfessionalPortalService portalService;
    private final IStudioService studioService;
    private final AuthHelper authHelper;

    // ── Dashboard ──────────────────────────────────────

    @GetMapping("/dashboard")
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

    // ── Availability ──────────────────────────────────────

    @GetMapping("/availability")
    public List<AvailabilityResponse> getAvailability(Authentication auth) {
        return portalService.getMyAvailability(
                authHelper.getProfessionalId(auth),
                authHelper.getStudioId(auth)
        );
    }

    @PutMapping("/availability")
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
