package com.prenota24.backend.controller;

import com.prenota24.backend.common.AuthHelper;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.dto.*;
import com.prenota24.backend.service.IAppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final IAppointmentService appointmentService;
    private final AuthHelper authHelper;

    @GetMapping
    public Page<AppointmentResponse> list(@PageableDefault(size = 20) Pageable pageable,
                                           Authentication auth) {
        return appointmentService.list(authHelper.getStudioId(auth), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@RequestBody @Valid CreateAppointmentRequest request,
                                       Authentication auth) {
        return appointmentService.create(request, authHelper.getStudioId(auth));
    }

    @GetMapping("/{id}")
    public AppointmentResponse getById(@PathVariable UUID id, Authentication auth) {
        return appointmentService.getById(id, authHelper.getStudioId(auth));
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable UUID id,
                                       @RequestBody @Valid UpdateAppointmentRequest request,
                                       Authentication auth) {
        return appointmentService.update(id, request, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable UUID id, Authentication auth) {
        return appointmentService.confirm(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id,
                                       @RequestBody(required = false) CancelAppointmentRequest request,
                                       Authentication auth) {
        return appointmentService.cancel(id, request, CancelledBy.PROFESSIONAL, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable UUID id, Authentication auth) {
        return appointmentService.complete(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/no-show")
    public AppointmentResponse noShow(@PathVariable UUID id, Authentication auth) {
        return appointmentService.noShow(id, authHelper.getStudioId(auth));
    }

    @PostMapping("/{id}/propose-new-time")
    public AppointmentResponse proposeNewTime(@PathVariable UUID id,
                                               @RequestBody @Valid ProposeNewTimeRequest request,
                                               Authentication auth) {
        return appointmentService.proposeNewTime(id, request, authHelper.getStudioId(auth));
    }
}
