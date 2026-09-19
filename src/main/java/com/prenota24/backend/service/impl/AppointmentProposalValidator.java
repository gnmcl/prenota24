package com.prenota24.backend.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.prenota24.backend.common.SlotNotAvailableException;
import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.dto.ProposeNewTimeRequest;
import com.prenota24.backend.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentProposalValidator {

    private final AppointmentRepository appointmentRepository;

    public void validate(Appointment appointment, ProposeNewTimeRequest request) {
        validateSlot(request.proposedStart(), request.proposedEnd(), "L'orario proposto", true);
        validateOptionalSlot(request.proposedStart2(), request.proposedEnd2(), "La seconda proposta");
        validateOptionalSlot(request.proposedStart3(), request.proposedEnd3(), "La terza proposta");
        validateDistinctSlots(request);
        checkConflict(appointment, request.proposedStart(), request.proposedEnd(), "L'orario proposto");
        checkOptionalSlotConflict(appointment, request.proposedStart2(), request.proposedEnd2(), "La seconda proposta");
        checkOptionalSlotConflict(appointment, request.proposedStart3(), request.proposedEnd3(), "La terza proposta");
    }

    private void validateOptionalSlot(Instant start, Instant end, String label) {
        if (start == null && end == null) {
            return;
        }
        validateSlot(start, end, label, false);
    }

    private void validateSlot(Instant start, Instant end, String label, boolean required) {
        if (start == null || end == null) {
            var suffix = required ? " deve includere inizio e fine" : " deve includere sia inizio sia fine";
            throw new IllegalArgumentException(label + suffix);
        }
        if (!end.isAfter(start)) {
            var message = required
                    ? "L'orario di fine deve essere successivo a quello di inizio"
                    : "L'orario di fine della proposta deve essere successivo a quello di inizio";
            throw new IllegalArgumentException(message);
        }
        if (!start.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Non è possibile proporre un orario passato");
        }
    }

    private void validateDistinctSlots(ProposeNewTimeRequest request) {
        if (sameSlot(request.proposedStart(), request.proposedEnd(), request.proposedStart2(), request.proposedEnd2())
                || sameSlot(request.proposedStart(), request.proposedEnd(), request.proposedStart3(), request.proposedEnd3())
                || sameSlot(request.proposedStart2(), request.proposedEnd2(), request.proposedStart3(), request.proposedEnd3())) {
            throw new IllegalArgumentException("Le proposte di orario devono essere distinte");
        }
    }

    private boolean sameSlot(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart != null && firstEnd != null
                && firstStart.equals(secondStart) && firstEnd.equals(secondEnd);
    }

    private void checkOptionalSlotConflict(Appointment appointment, Instant start, Instant end, String label) {
        if (start != null) {
            checkConflict(appointment, start, end, label);
        }
    }

    private void checkConflict(Appointment appointment, Instant start, Instant end, String label) {
        var conflicts = appointmentRepository.countConflictingAppointments(
                appointment.getProfessional().getId(), start, end, appointment.getId());
        if (conflicts > 0) {
            throw new SlotNotAvailableException(label + " si sovrappone con un altro appuntamento");
        }
    }
}
