package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prenota24.backend.common.BookingLimitExceededException;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.dto.ProfessionalResponse;
import com.prenota24.backend.dto.PublicBookingRequest;
import com.prenota24.backend.repository.AppointmentRepository;
import com.prenota24.backend.repository.ServiceTypeRepository;
import com.prenota24.backend.repository.StudioRepository;
import com.prenota24.backend.service.IAppointmentService;
import com.prenota24.backend.service.IClientService;
import com.prenota24.backend.service.IProfessionalService;
import com.prenota24.backend.service.IServiceTypeService;

class PublicBookingLimitTest {

    private final StudioRepository studioRepository = mock(StudioRepository.class);
    private final IProfessionalService professionalService = mock(IProfessionalService.class);
    private final IClientService clientService = mock(IClientService.class);
    private final IAppointmentService appointmentService = mock(IAppointmentService.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);

    private final PublicBookingService service = new PublicBookingService(
            studioRepository,
            mock(ServiceTypeRepository.class),
            professionalService,
            mock(IServiceTypeService.class),
            clientService,
            appointmentService,
            mock(ServiceTypeAssignmentValidator.class),
            appointmentRepository);

    @Test
    void rejectsBookingWhenEmailAlreadyHasTooManyPendingRequests() {
        var studio = Studio.builder().id(UUID.randomUUID()).slug("studio").timezone("Europe/Rome").build();
        var professionalId = UUID.randomUUID();
        when(studioRepository.findBySlug("studio")).thenReturn(Optional.of(studio));
        when(professionalService.getById(professionalId, studio.getId())).thenReturn(
                new ProfessionalResponse(professionalId, studio.getId(), "Ada", "Rossi", null, null, true, Instant.now()));
        when(appointmentRepository.countFutureByStudioAndClientEmailAndStatusIn(
                eq(studio.getId()), eq("spam@example.com"), any(), any()))
                .thenReturn((long) PublicBookingService.MAX_PENDING_PUBLIC_BOOKINGS_PER_EMAIL);

        var start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        var request = new PublicBookingRequest(professionalId, null, start, start.plus(30, ChronoUnit.MINUTES),
                "Mario", "Rossi", " spam@example.com ", null, null);

        assertThatThrownBy(() -> service.createAppointment("studio", request))
                .isInstanceOf(BookingLimitExceededException.class);
        verify(professionalService, never()).getAvailableSlots(any(), any(), anyInt(), any());
        verify(clientService, never()).findOrCreateFromReservation(any(), any(), any(), any());
        verify(appointmentService, never()).create(any(), any());
    }
}
