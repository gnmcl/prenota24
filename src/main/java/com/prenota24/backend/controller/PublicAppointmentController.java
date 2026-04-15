package com.prenota24.backend.controller;

import com.prenota24.backend.dto.AppointmentResponse;
import com.prenota24.backend.dto.CancelAppointmentRequest;
import com.prenota24.backend.domain.CancelledBy;
import com.prenota24.backend.service.IAppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/appointments")
@RequiredArgsConstructor
@Tag(name = "Public Appointments", description = "Operazioni pubbliche tramite token appuntamento (accetta/rifiuta proposta, cancella)")
@SecurityRequirements
public class PublicAppointmentController {

    private final IAppointmentService appointmentService;

    @GetMapping("/{token}")
    @Operation(summary = "Dettaglio appuntamento via token")
    public AppointmentResponse getByToken(@PathVariable String token) {
        return appointmentService.getByToken(token);
    }

    @PostMapping("/{token}/accept")
    @Operation(summary = "Accetta proposta nuovo orario", description = "Transizione: PROPOSED_NEW_TIME → CONFIRMED")
    public AppointmentResponse acceptProposal(@PathVariable String token) {
        return appointmentService.acceptProposal(token);
    }

    @PostMapping("/{token}/reject")
    @Operation(summary = "Rifiuta proposta nuovo orario", description = "Transizione: PROPOSED_NEW_TIME → REQUESTED")
    public AppointmentResponse rejectProposal(@PathVariable String token) {
        return appointmentService.rejectProposal(token);
    }

    @PostMapping("/{token}/cancel")
    @Operation(summary = "Cancella appuntamento (lato cliente)", description = "Il cliente cancella tramite token pubblico")
    public AppointmentResponse cancel(@PathVariable String token,
                                       @RequestBody(required = false) CancelAppointmentRequest request) {
        var appointment = appointmentService.getByToken(token);
        return appointmentService.cancel(appointment.id(), request, CancelledBy.CLIENT, appointment.studioId());
    }
}
