package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prenota24.backend.common.SlotNotAvailableException;
import com.prenota24.backend.domain.Appointment;
import com.prenota24.backend.domain.Professional;
import com.prenota24.backend.dto.ProposeNewTimeRequest;
import com.prenota24.backend.repository.AppointmentRepository;

class AppointmentProposalValidatorTest {

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final AppointmentProposalValidator validator = new AppointmentProposalValidator(appointmentRepository);

    @Test
    void rejectsIncompleteOptionalProposal() {
        var start = Instant.now().plus(1, ChronoUnit.DAYS);
        var request = new ProposeNewTimeRequest(
                start,
                start.plus(1, ChronoUnit.HOURS),
                start.plus(2, ChronoUnit.HOURS),
                null,
                null,
                null);

        assertThatThrownBy(() -> validator.validate(appointment(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seconda proposta");
    }

    @Test
    void checksConflictsOnOptionalProposals() {
        var start = Instant.now().plus(1, ChronoUnit.DAYS);
        var secondStart = start.plus(2, ChronoUnit.HOURS);
        var appointment = appointment();
        var request = new ProposeNewTimeRequest(
                start,
                start.plus(1, ChronoUnit.HOURS),
                secondStart,
                secondStart.plus(1, ChronoUnit.HOURS),
                null,
                null);
        when(appointmentRepository.countConflictingAppointments(
                appointment.getProfessional().getId(),
                secondStart,
                secondStart.plus(1, ChronoUnit.HOURS),
                appointment.getId())).thenReturn(1L);

        assertThatThrownBy(() -> validator.validate(appointment, request))
                .isInstanceOf(SlotNotAvailableException.class)
                .hasMessageContaining("seconda proposta");
    }

    private Appointment appointment() {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .professional(Professional.builder().id(UUID.randomUUID()).build())
                .build();
    }
}
