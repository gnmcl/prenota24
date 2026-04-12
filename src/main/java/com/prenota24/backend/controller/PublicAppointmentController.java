package com.prenota24.backend.controller;

import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.service.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/appointments")
@RequiredArgsConstructor
public class PublicAppointmentController {

    private final IAppointmentService appointmentService;

    @GetMapping("/{token}")
    public AppointmentResponse getByToken(@PathVariable String token) {
        return appointmentService.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    public AppointmentResponse acceptProposal(@PathVariable String token) {
        return appointmentService.acceptProposal(token);
    }

    @PostMapping("/{token}/reject")
    public AppointmentResponse rejectProposal(@PathVariable String token) {
        return appointmentService.rejectProposal(token);
    }

    @PostMapping("/{token}/cancel")
    public AppointmentResponse cancel(@PathVariable String token,
                                       @RequestBody(required = false) CancelAppointmentRequest request) {
        // For public cancel, we find by token, but use the underlying cancel logic
        var appointment = appointmentService.getByToken(token);
        return appointmentService.cancel(appointment.id(), request, CancelledBy.CLIENT, appointment.studioId());
    }
}
