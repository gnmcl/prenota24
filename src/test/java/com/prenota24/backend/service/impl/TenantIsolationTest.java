package com.prenota24.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.prenota24.backend.common.EntityNotFoundException;
import com.prenota24.backend.domain.AppUser;
import com.prenota24.backend.domain.Event;
import com.prenota24.backend.domain.Reservation;
import com.prenota24.backend.domain.ReservationStatus;
import com.prenota24.backend.domain.Studio;
import com.prenota24.backend.domain.UserRole;
import com.prenota24.backend.dto.CreateAppUserRequest;
import com.prenota24.backend.repository.AppUserRepository;
import com.prenota24.backend.repository.EventRepository;
import com.prenota24.backend.repository.ReservationRepository;
import com.prenota24.backend.repository.StudioRepository;

class TenantIsolationTest {

    // ── Creazione utenti ──────────────────────────────────────

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final StudioRepository studioRepository = mock(StudioRepository.class);
    private final AppUserService appUserService =
            new AppUserService(appUserRepository, studioRepository, new BCryptPasswordEncoder(4));

    @Test
    void createdUserAlwaysBelongsToTheAdminStudioIgnoringRequestStudioId() {
        var adminStudio = Studio.builder().id(UUID.randomUUID()).name("Mine").build();
        var victimStudioId = UUID.randomUUID();
        when(appUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(studioRepository.findById(adminStudio.getId())).thenReturn(Optional.of(adminStudio));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        appUserService.create(
                new CreateAppUserRequest(victimStudioId, "New@Example.com", "password123", UserRole.ADMIN),
                adminStudio.getId());

        var saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());
        assertThat(saved.getValue().getStudio().getId()).isEqualTo(adminStudio.getId());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        verify(studioRepository, never()).findById(victimStudioId);
    }

    // ── Prenotazioni evento ──────────────────────────────────────

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final ReservationService reservationService =
            new ReservationService(reservationRepository, eventRepository);

    @Test
    void nonOwnerCannotListEventReservations() {
        var event = eventOwnedBy(UUID.randomUUID());
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> reservationService.getByEventId(event.getId(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(reservationRepository, never()).findByEventIdOrderByCreatedAtDesc(any());
    }

    @Test
    void ownerCanListEventReservations() {
        var ownerId = UUID.randomUUID();
        var event = eventOwnedBy(ownerId);
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(reservationRepository.findByEventIdOrderByCreatedAtDesc(event.getId()))
                .thenReturn(List.of(reservation(event)));

        assertThat(reservationService.getByEventId(event.getId(), ownerId)).hasSize(1);
    }

    @Test
    void nonOwnerCannotCancelReservation() {
        var reservation = reservation(eventOwnedBy(UUID.randomUUID()));
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(reservation.getId(), UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationRepository, never()).save(any());
    }

    private Event eventOwnedBy(UUID ownerId) {
        return Event.builder()
                .id(UUID.randomUUID())
                .createdBy(AppUser.builder().id(ownerId).build())
                .build();
    }

    private Reservation reservation(Event event) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .event(event)
                .guestName("Guest")
                .guestEmail("guest@example.com")
                .status(ReservationStatus.CONFIRMED)
                .build();
    }
}
